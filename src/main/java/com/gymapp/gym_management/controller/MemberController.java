package com.gymapp.gym_management.controller;

import com.gymapp.gym_management.entity.Member;
import com.gymapp.gym_management.entity.Plan;
import com.gymapp.gym_management.repository.PlanRepository;
import com.gymapp.gym_management.repository.MemberRepository;
import com.gymapp.gym_management.service.UserService;
import com.gymapp.gym_management.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.time.LocalDate;
import com.gymapp.gym_management.dto.MemberRequest;
import com.gymapp.gym_management.dto.RenewRequest;

import org.springframework.web.multipart.MultipartFile;
import com.opencsv.CSVReader;
import java.io.InputStreamReader;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberRepository memberRepository;
    private final PlanRepository planRepository;
    private final UserService userService;

    public MemberController(MemberRepository memberRepository, PlanRepository planRepository, UserService userService) {
        this.memberRepository = memberRepository;
        this.planRepository = planRepository;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<?> addMember(@RequestBody MemberRequest req) {
        try {
            Long userId = userService.getCurrentUserId();
            
            Member member = new Member();
            member.setName(req.getName());
            member.setEmail(req.getEmail());
            member.setPhone(req.getPhone());
            member.setStartDate(req.getStartDate());
            member.setEndDate(req.getEndDate());
            member.setUserId(userId);

            if (req.getPlanId() != null) {
                // Verify plan belongs to current user
                Plan plan = planRepository.findByIdAndUserId(req.getPlanId(), userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", req.getPlanId()));
                member.setPlan(plan);

                // auto-calc endDate when not provided
                if (member.getEndDate() == null && member.getStartDate() != null) {
                    member.setEndDate(member.getStartDate().plusMonths(plan.getDurationInMonths()));
                }
            } else {
                return ResponseEntity.badRequest().body("planId is required");
            }

            Member saved = memberRepository.save(member);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error saving member: " + e.getMessage());
        }
    }


    @GetMapping
    public List<Member> getAllMembers() {
        Long userId = userService.getCurrentUserId();
        return memberRepository.findByUserId(userId);
    }

    @GetMapping("/expiring")
    public List<Member> getExpiringMembers(@RequestParam(defaultValue = "7") int days) {
        Long userId = userService.getCurrentUserId();
        LocalDate today = LocalDate.now();
        LocalDate upcoming = today.plusDays(days);
        return memberRepository.findByUserIdAndEndDateBetween(userId, today, upcoming);
    }

    @PutMapping("/{id}/renew")
    public ResponseEntity<?> renewMembership(@PathVariable Long id, @RequestBody(required = false) RenewRequest body) {
        Long userId = userService.getCurrentUserId();
        Optional<Member> memberOpt = memberRepository.findByIdAndUserId(id, userId);
        if (memberOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Member not found");
        }

        Member member = memberOpt.get();
        LocalDate today = LocalDate.now();
        LocalDate lastEnd = member.getEndDate();

        // Check if user wants to change the plan
        if (body != null && body.getPlanId() != null) {
            Plan newPlan = planRepository.findByIdAndUserId(body.getPlanId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", body.getPlanId()));
            member.setPlan(newPlan);
        }

        // Calculate new start date
        LocalDate newStart;
        if (today.isAfter(lastEnd)) {
            newStart = today;
        } else {
            newStart = lastEnd.plusDays(1);
        }
        member.setStartDate(newStart);

        // Calculate end date
        LocalDate newEndDate;
        if (body != null && body.getNewEndDate() != null) {
            newEndDate = body.getNewEndDate();
        } else {
            newEndDate = newStart.plusMonths(member.getPlan().getDurationInMonths());
        }

        member.setEndDate(newEndDate);
        Member saved = memberRepository.save(member);
        
        return ResponseEntity.ok(saved);
    }


    @GetMapping("/export")
    public void exportMembers(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=members.csv");

        Long userId = userService.getCurrentUserId();
        List<Member> members = memberRepository.findByUserId(userId);
        PrintWriter writer = response.getWriter();

        // Write CSV header
        writer.println("ID,Name,Email,Phone,Plan,Start Date,End Date");

        // Write member data
        for (Member m : members) {
            writer.println(String.format("%d,%s,%s,%s,%s,%s,%s",
                    m.getId(),
                    m.getName(),
                    m.getEmail(),
                    m.getPhone(),
                    m.getPlan().getPlanName(),
                    m.getStartDate(),
                    m.getEndDate()
            ));
        }

        writer.flush();
    }

    @PostMapping("/import")
public ResponseEntity<String> importMembers(@RequestParam("file") MultipartFile file) {
  if (file.isEmpty()) {
      return ResponseEntity.badRequest().body("Please upload a CSV file");
  }

  Long userId = userService.getCurrentUserId();
  int successCount = 0;
  int failedCount = 0;
  List<String> errors = new ArrayList<>();

  try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
      String[] headers = reader.readNext(); // header row
      if (headers == null) {
          return ResponseEntity.badRequest().body("Empty CSV file");
      }

      // Build header map (case-insensitive)
      Map<String, Integer> headerMap = new HashMap<>();
      for (int i = 0; i < headers.length; i++) {
          headerMap.put(headers[i].trim().toLowerCase(), i);
      }

      // Required columns
      List<String> requiredHeaders = List.of("name", "phone", "planid", "startdate", "enddate");
      for (String h : requiredHeaders) {
          if (!headerMap.containsKey(h)) {
              return ResponseEntity.badRequest()
                      .body("Missing required column: " + h + "\nPresent: " + headerMap.keySet());
          }
      }

      String[] line;
      int rowNumber = 1; // header is row 1

      while ((line = reader.readNext()) != null) {
          rowNumber++;

          // Skip completely empty lines
          boolean allEmpty = true;
          for (String v : line) {
              if (v != null && !v.trim().isEmpty()) {
                  allEmpty = false;
                  break;
              }
          }
          if (allEmpty) continue;

          try {
              // Safe extraction with bounds checking
              Integer nameIdx = headerMap.get("name");
              Integer phoneIdx = headerMap.get("phone");
              Integer emailIdx = headerMap.get("email");
              Integer planIdIdx = headerMap.get("planid");
              Integer startDateIdx = headerMap.get("startdate");
              Integer endDateIdx = headerMap.get("enddate");

              String name = (nameIdx != null && nameIdx < line.length) 
                      ? line[nameIdx].trim() 
                      : "";
              String phone = (phoneIdx != null && phoneIdx < line.length) 
                      ? line[phoneIdx].trim() 
                      : "";
              String email = (emailIdx != null && emailIdx < line.length) 
                      ? line[emailIdx].trim() 
                      : null;
              String planIdRaw = (planIdIdx != null && planIdIdx < line.length) 
                      ? line[planIdIdx].trim() 
                      : "";
              String startDateRaw = (startDateIdx != null && startDateIdx < line.length) 
                      ? line[startDateIdx].trim() 
                      : "";
              String endDateRaw = (endDateIdx != null && endDateIdx < line.length) 
                      ? line[endDateIdx].trim() 
                      : "";

              // Convert empty email to null
              if (email != null && email.isEmpty()) {
                  email = null;
              }

              if (name.isEmpty()) {
                  throw new RuntimeException("Name is required");
              }
              if (phone.isEmpty()) {
                  throw new RuntimeException("Phone is required");
              }
              if (planIdRaw.isEmpty()) {
                  throw new RuntimeException("planId is required");
              }
              if (startDateRaw.isEmpty()) {
                  throw new RuntimeException("startDate is required");
              }
              if (endDateRaw.isEmpty()) {
                  throw new RuntimeException("endDate is required");
              }

              Long planId = Long.parseLong(planIdRaw);
              LocalDate startDate = parseFlexibleDate(startDateRaw);
              LocalDate endDate = parseFlexibleDate(endDateRaw);

              // Verify plan belongs to current user
              Plan plan = planRepository.findByIdAndUserId(planId, userId)
                      .orElseThrow(() -> new RuntimeException("Plan not found for ID: " + planId));

              Member member = new Member();
              member.setName(name);
              member.setEmail(email);
              member.setPhone(phone);
              member.setPlan(plan);
              member.setStartDate(startDate);
              member.setEndDate(endDate);
              member.setUserId(userId);
              memberRepository.save(member);
              successCount++;

          } catch (Exception ex) {
              failedCount++;
              // Include context in error message
              String rowContext = "";
              try {
                  Integer nameIdx = headerMap.get("name");
                  if (nameIdx != null && nameIdx < line.length) {
                      rowContext = " (Name: " + line[nameIdx].trim() + ")";
                  }
              } catch (Exception ignored) {}
              errors.add("Row " + rowNumber + rowContext + ": " + ex.getMessage());
          }
      }

  } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.internalServerError()
              .body("Error while importing: " + e.getMessage());
  }

  // Final summary
  StringBuilder summary = new StringBuilder();
  summary.append("✅ Successfully imported: ").append(successCount)
         .append("\n⚠️ Failed: ").append(failedCount);

  if (!errors.isEmpty()) {
      summary.append("\n\nError details:\n");
      errors.forEach(err -> summary.append("• ").append(err).append("\n"));
  }

  return ResponseEntity.ok(summary.toString());
}

    /**
     * Parse date from various formats commonly found in CSV/Excel files
     */
    private LocalDate parseFlexibleDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            throw new RuntimeException("Date cannot be empty");
        }

        dateStr = dateStr.trim();

        // Common date formats to try
        DateTimeFormatter[] formatters = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),        // 2025-01-15
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),        // 15/01/2025
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),        // 01/15/2025
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),        // 15-01-2025
            DateTimeFormatter.ofPattern("MM-dd-yyyy"),        // 01-15-2025
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),        // 2025/01/15
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),        // 15.01.2025
            DateTimeFormatter.ofPattern("d/M/yyyy"),          // 5/1/2025
            DateTimeFormatter.ofPattern("M/d/yyyy"),          // 1/5/2025
            DateTimeFormatter.ofPattern("d-M-yyyy"),          // 5-1-2025
            DateTimeFormatter.ofPattern("yyyy-M-d"),          // 2025-1-5
        };

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException e) {
                // Try next format
            }
        }

        throw new RuntimeException("Invalid date format: '" + dateStr + "'. Supported formats: yyyy-MM-dd, dd/MM/yyyy, MM/dd/yyyy, dd-MM-yyyy, etc.");
    }

    // Update member (edit)
@PutMapping("/{id}")
public ResponseEntity<?> updateMember(@PathVariable Long id, @RequestBody MemberRequest req) {
    Long userId = userService.getCurrentUserId();
    Member member = memberRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Member", "id", id));

    // Basic fields
    member.setName(req.getName());
    member.setPhone(req.getPhone());
    member.setEmail(req.getEmail());

    // Plan change (if planId provided)
    if (req.getPlanId() != null) {
        // Verify plan belongs to current user
        Plan plan = planRepository.findByIdAndUserId(req.getPlanId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", req.getPlanId()));
        member.setPlan(plan);

        // If startDate present and endDate not sent → auto calc end date based on plan
        if (req.getStartDate() != null && req.getEndDate() == null) {
            member.setStartDate(req.getStartDate());
            member.setEndDate(req.getStartDate().plusMonths(plan.getDurationInMonths()));
        }
    }

    // Dates (if explicitly provided)
    if (req.getStartDate() != null) {
        member.setStartDate(req.getStartDate());
    }
    if (req.getEndDate() != null) {
        member.setEndDate(req.getEndDate());
    }

    Member saved = memberRepository.save(member);
    return ResponseEntity.ok(saved);
}

    // Delete member
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteMember(@PathVariable Long id) {
        Long userId = userService.getCurrentUserId();
        memberRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", "id", id));
        memberRepository.deleteById(id);
        return ResponseEntity.ok("Member deleted");
    }




}
