package com.sbpl.OPD.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.util.Date;
import java.util.UUID;

/**
 * This Is A Base Entity Class For All Other Mapped Entity.
 *
 * @author Rahul Kumar
 */
@MappedSuperclass
public abstract class BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "request_id", nullable = false)
  private String requestId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Date createdAt;

  @Column(name = "updated_at", nullable = false)
  private Date updatedAt;

  @Column(name = "created_at_ms", nullable = false, updatable = false)
  private long createdAtMs;

  @Column(name = "updated_at_ms", nullable = false)
  private long updatedAtMs;

  @Column(name = "created_by")
  private Long createdBy;

  @Column(name = "updated_by")
  private Long updatedBy;

  // Getters and Setters
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  
  public String getRequestId() { return requestId; }
  public void setRequestId(String requestId) { this.requestId = requestId; }
  
  public Date getCreatedAt() { return createdAt; }
  public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
  
  public Date getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
  
  public long getCreatedAtMs() { return createdAtMs; }
  public void setCreatedAtMs(long createdAtMs) { this.createdAtMs = createdAtMs; }
  
  public long getUpdatedAtMs() { return updatedAtMs; }
  public void setUpdatedAtMs(long updatedAtMs) { this.updatedAtMs = updatedAtMs; }

  public Long getCreatedBy() { return createdBy; }
  public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

  public Long getUpdatedBy() { return updatedBy; }
  public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }

  /**
   * Method for before Insert set the Time and active key.
   */
  @PrePersist
  public void beforeInsert() {
    Date currentDateTime = new Date();
    this.requestId = UUID.randomUUID().toString();
    this.createdAtMs = currentDateTime.getTime();
    this.updatedAtMs = currentDateTime.getTime();
    this.createdAt = currentDateTime;
    this.updatedAt = currentDateTime;
  }

  /**
   * Method for before Update set the Updated Time.
   */
  @PreUpdate
  public void beforeUpdate() {
    Date currentDateTime = new Date();
    this.updatedAtMs = currentDateTime.getTime();
    this.updatedAt = currentDateTime;
  }
}