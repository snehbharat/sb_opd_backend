package com.sbpl.OPD.utils;

import com.sbpl.OPD.Auth.enums.UserRole;
import com.sbpl.OPD.Auth.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * This Is a Db utill class.
 *
 * @author Rahul Kumar
 */
public class DbUtill {  // Renamed from DbUtill to DbUtill to match filename

    /**
     * Returns the start and end time of the current day in IST (Indian Standard Time)
     * as epoch milliseconds.
     * This method is independent of the server time zone. Even if the server
     * is running in UTC, the calculation is always performed using IST.
     * The returned map contains:
     * - startOfDayMs : start of today (00:00:00.000 IST)
     * - endOfDayMs   : end of today (23:59:59.999 IST)
     *
     * @return a map containing IST start and end time of the current day in milliseconds
     */
    public static Map<String, Long> getTodayStartDateMiliAndEndDateMili() {

        ZoneId istZone = ZoneId.of("Asia/Kolkata");

        long startOfDayMs = LocalDate.now(istZone)
                .atStartOfDay(istZone)
                .toInstant()
                .toEpochMilli();

        long endOfDayMs = LocalDate.now(istZone)
                .atTime(23, 59, 59, 999_000_000)
                .atZone(istZone)
                .toInstant()
                .toEpochMilli();

        Map<String, Long> result = new HashMap<>();
        result.put("startOfDayMs", startOfDayMs);
        result.put("endOfDayMs", endOfDayMs);

        return result;
    }

    /**
     * Get logged-in user's company ID.
     *
     * Used for multi-tenant data isolation.
     *
     * @return companyId of logged-in user
     */
    public static Long getLoggedInCompanyId() {

        User user = getCurrentUser();

        if (user.getCompany() == null) {
            throw new IllegalStateException("User is not associated with any company");
        }

        return user.getCompany().getId();
    }

    /**
     * Get The Branch Id For This Logged In user.
     *
     * @return @{@link Long}
     */
    public static Long getLoggedInBranchId() {

        User user = getCurrentUser();

        if (user.getBranch() == null) {
            throw new IllegalStateException("User is not associated with any Branch");
        }

        return user.getBranch().getId();
    }

    public static String getLoggedInUserRole() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        return authentication.getAuthorities()
                .stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No role found"));
    }

    /**
     * Get The Role For This Logged In user.
     *
     * @return @{@link Long}
     */
    public static UserRole getLoggedInUserOriginalRole() {

        User user = getCurrentUser();

        return user.getRole();
    }


    /**
     * Fetch logged-in user's ID from Spring Security context.
     *
     * @return logged-in user ID
     */
    public static Long getLoggedInUserId() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof User user) {
            return user.getId();
        }

        throw new IllegalStateException("Invalid authentication principal");
    }

    /**
     * Get the currently authenticated user from security context
     *
     * @return User object of the currently logged-in user
     */
    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof User user) {
            return user;
        }

        throw new IllegalStateException("Invalid authentication principal");
    }

    private static String mapSortField(String field) {
      return switch (field) {
        case "created_at" -> "created_at";
        case "updated_at" -> "updated_at";
        case "createdAt" -> "createdAt";
        default -> field;
      };
    }

    public static PageRequest createPageRequest(Integer pageNo, Integer pageSize) {
        int page = pageNo != null ? pageNo : 0;
        int size = pageSize != null ? pageSize : 10;
        String sortField = mapSortField("created_at");
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortField));
    }

    public static PageRequest buildPageRequest(Integer pageNo, Integer pageSize) {
        int page = pageNo != null ? pageNo : 0;
        int size = pageSize != null ? pageSize : 10;
        String sortField = mapSortField("createdAt");
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortField));
    }

    public static PageRequest buildPageRequestWithSort(Integer pageNo, Integer pageSize, String sortBy, Sort.Direction direction) {
        int page = pageNo != null ? pageNo : 0;
        int size = pageSize != null ? pageSize : 10;
        String sortField = sortBy != null ? mapSortField(sortBy) : mapSortField("created_at");
        Sort.Direction sortDirection = direction != null ? direction : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(sortDirection, sortField));
    }

    public static PageRequest buildPageRequestWithDefaultSort(Integer pageNo, Integer pageSize) {
        return buildPageRequestWithSort(pageNo, pageSize, "createdAt", Sort.Direction.DESC);
    }

    /**
     * Build page request with default sort for JPQL queries
     * Uses entity field names instead of database column names
     */
    public static PageRequest buildPageRequestWithDefaultSortForJPQL(Integer pageNo, Integer pageSize) {
        int page = pageNo != null ? pageNo : 0;
        int size = pageSize != null ? pageSize : 10;
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    /**
     * Build page request without additional sorting
     * For use with native queries that already have ORDER BY clauses
     */
    public static PageRequest buildPageRequestWithoutSort(Integer pageNo, Integer pageSize) {
        int page = pageNo != null ? pageNo : 0;
        int size = pageSize != null ? pageSize : 10;
        return PageRequest.of(page, size); // No sorting specified
    }

    public static Map<String, Object> buildPaginatedResponse(Object page, java.util.List<?> content) {
        Map<String, Object> response = new HashMap<>(8, 1);
        if (page instanceof Page<?> p) {
          response.put("content", content);
            response.put("pageNo", p.getNumber());
            response.put("pageSize", p.getSize());
            response.put("totalElements", p.getTotalElements());
            response.put("totalPages", p.getTotalPages());
            response.put("first", p.isFirst());
            response.put("last", p.isLast());
        }
        return response;
    }
}