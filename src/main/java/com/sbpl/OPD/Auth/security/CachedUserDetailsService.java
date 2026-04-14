package com.sbpl.OPD.Auth.security;//package com.sbpl.OPD.security;
//
//import com.sbpl.OPD.Auth.model.User;
//import com.sbpl.OPD.Auth.repository.UserRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.cache.annotation.Cacheable;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.stereotype.Service;
//
//@Service
//public class CachedUserDetailsService implements UserDetailsService {
//
//    @Autowired
//    private UserRepository userRepository;
//
//    @Override
//    @Cacheable(value = "users", key = "#username")
//    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
//        User user = userRepository.findByUsername(username)
//                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
//
//        // Ensure permissions are loaded to avoid lazy loading issues
//        if (user.getUserPermissions() != null) {
//            user.getUserPermissions().size(); // Trigger lazy loading
//        }
//
//        return user;
//    }
//
//    public void evictUserFromCache(String username) {
//        // This would be used when user details are updated
//    }
//}