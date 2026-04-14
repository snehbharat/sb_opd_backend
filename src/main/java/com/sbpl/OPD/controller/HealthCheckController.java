package com.sbpl.OPD.controller;

import com.sbpl.OPD.service.HealthCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * this is a controller class for application health check .
 *
 * @author kousik manik
 */
@RestController
@RequestMapping("/api/v1/health-check")
public class HealthCheckController {

  @Autowired
  private HealthCheckService healthCheckService;

  @GetMapping("/status")
  public String checkApplicationHealth() {
    return healthCheckService.checkApplicationHealth();
  }
}
