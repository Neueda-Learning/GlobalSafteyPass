package com.travelassistant.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="customers",uniqueConstraints=@UniqueConstraint(name="uk_customers_display_name",columnNames="display_name"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Customer {
    @Id private String id;
    @Column(name="display_name",nullable=false) private String displayName;
}
