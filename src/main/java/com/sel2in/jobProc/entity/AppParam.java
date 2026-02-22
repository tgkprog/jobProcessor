package com.sel2in.jobProc.entity;

import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "AppParams")
@Data
public class AppParam {
    
    @Id
    @Column(name = "param_name")
    private String name;

    @Column(name = "param_value")
    private String value;

    private String description;

    @Column(name = "updated_ts")
    private LocalDateTime updatedTs;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedTs = LocalDateTime.now();
    }
}
