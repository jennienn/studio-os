package com.studioos.studio.configuration;

import jakarta.persistence.*;
import java.util.UUID;
@Entity @Table(name="studio_capabilities")
public class StudioCapability {
    @Id public UUID id;
    @Column(nullable=false) public UUID studioId;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=32) public StudioTaxonomy.Capability capability;
    @Column(nullable=false) public boolean enabled;
    protected StudioCapability() {}
    public StudioCapability(UUID studioId,StudioTaxonomy.Capability capability) {
        this.id=UUID.randomUUID(); this.studioId=studioId; this.capability=capability;
    }
}
