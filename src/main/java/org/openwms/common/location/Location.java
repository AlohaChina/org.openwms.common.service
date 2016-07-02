/*
 * openwms.org, the Open Warehouse Management System.
 * Copyright (C) 2014 Heiko Scherrer
 *
 * This file is part of openwms.org.
 *
 * openwms.org is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * openwms.org is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.openwms.common.location;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.ameba.integration.jpa.BaseEntity;
import org.openwms.core.values.Message;

/**
 * A Location, represents a physical or virtual place in a warehouse. Could be something like a storage location in the stock or a location
 * on a conveyor. Error locations can be modeled with a Location entity, too. Multiple Locations are grouped to a {@link LocationGroup}.
 *
 * @author <a href="mailto:scherrer@openwms.org">Heiko Scherrer</a>
 * @version 1.0
 * @GlossaryTerm
 * @see org.openwms.common.location.LocationGroup
 * @since 0.1
 */
@Entity
@Table(name = "COM_LOCATION", uniqueConstraints = @UniqueConstraint(columnNames = {"C_AREA", "C_AISLE", "C_X", "C_Y", "C_Z"}))
public class Location extends BaseEntity implements Serializable {

    /** Unique natural key. */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "area", column = @Column(name = "C_AREA")),
            @AttributeOverride(name = "aisle", column = @Column(name = "C_AISLE")),
            @AttributeOverride(name = "x", column = @Column(name = "C_X")),
            @AttributeOverride(name = "y", column = @Column(name = "C_Y")),
            @AttributeOverride(name = "z", column = @Column(name = "C_Z"))
    })
    private LocationPK locationId;

    /** Description of the Location. */
    @Column(name = "C_DESCRIPTION")
    private String description;

    /** Maximum number of {@code TransportUnit}s placed on this Location. */
    @Column(name = "C_NO_MAX_TRANSPORT_UNITS")
    private short noMaxTransportUnits = 1;

    /** Maximum allowed weight on this Location. */
    @Column(name = "C_MAXIMUM_WEIGHT")
    private BigDecimal maximumWeight;

    /**
     * Date of last change. When a {@code TransportUnit} is moving to or away from this Location, {@code lastAccess} will be updated. This
     * is useful to get the history of {@code TransportUnit}s as well as for inventory calculation.
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "C_LAST_ACCESS")
    private Date lastAccess;

    /** Flag to indicate whether {@code TransportUnit}s should be counted on this Location or not. */
    @Column(name = "C_COUNTING_ACTIVE")
    private boolean countingActive = false;

    /** Reserved for stock check procedure and inventory calculation. */
    @Column(name = "C_CHECK_STATE")
    private String checkState = DEF_CHECK_STATE;
    /** Default value of {@code Location#checkstate}. */
    public static final String DEF_CHECK_STATE = "--";

    /**
     * Shall this Location be integrated in the calculation of {@code TransportUnit}s of the parent {@link LocationGroup}.<ul><li>{@literal
     * true} : Location is included in calculation of {@code TransportUnit}s.</li><li>{@literal false}: Location is not included in
     * calculation of {@code TransportUnit}s.</li></ul>
     */
    @Column(name = "C_LG_COUNTING_ACTIVE")
    private boolean locationGroupCountingActive = false;

    /**
     * Signals the incoming state of this Location. Locations which are blocked for incoming cannot pick up {@code TransportUnit}s.<ul>
     * <li>{@literal true} : Location is ready to pick up {@code TransportUnit}s.</li> <li>{@literal false}: Location is locked, and cannot
     * pick up {@code TransportUnit}s.</li></ul>
     */
    @Column(name = "C_INCOMING_ACTIVE")
    private boolean incomingActive = true;

    /**
     * Signals the outgoing state of this Location. Locations which are blocked for outgoing cannot release {@code
     * TransportUnit}s.<ul><li>{@literal true} : Location is enabled for outgoing {@code TransportUnit}s.</li><li>{@literal false}: Location
     * is locked, {@code TransportUnit}s can't leave this Location.</li></ul>
     */
    @Column(name = "C_OUTGOING_ACTIVE")
    private boolean outgoingActive = true;

    /**
     * The PLC is able to change the state of a Location. This property stores the last state, received from the PLC.<ul><li>-1: Not
     * defined.</li><li>0 : No PLC error, everything okay.</li></ul>
     */
    @Column(name = "C_PLC_STATE")
    private short plcState = 0;

    /**
     * Determines whether the Location is considered in the allocation procedure.<ul><li>{@literal true} : This Location will be considered
     * in storage calculation by an allocation procedure.</li><li>{@literal false} : This Location will not be considered in the allocation
     * process.</li></ul>
     */
    @Column(name = "C_CONSIDERED_IN_ALLOCATION")
    private boolean consideredInAllocation = true;

    /** The {@link LocationType} this Location belongs to. */
    @ManyToOne
    @JoinColumn(name = "C_LOCATION_TYPE")
    private LocationType locationType;

    /** The {@link LocationGroup} this Location belongs to. */
    @ManyToOne
    @JoinColumn(name = "C_LOCATION_GROUP")
    private LocationGroup locationGroup;

    /** Stored {@link Message}s on this Location. */
    @OneToMany(cascade = {CascadeType.ALL})
    @JoinTable(name = "COM_LOCATION_MESSAGE", joinColumns = @JoinColumn(name = "C_LOCATION_ID"), inverseJoinColumns = @JoinColumn(name = "C_MESSAGE_ID"))
    private Set<Message> messages = new HashSet<>();

    /*~ ----------------------------- constructors ------------------- */
    /**
     * Create a new Location with the business key.
     *
     * @param locationId The unique natural key of the Location
     */
    public Location(LocationPK locationId) {
        this.locationId = locationId;
    }
    
    /** Dear JPA... */
    protected Location() {
    }

    /*~ ----------------------------- methods ------------------- */
    /**
     * Add a new {@link Message} to this Location.
     *
     * @param message The {@link Message} to be added
     * @return {@literal true} if the {@link Message} is new in the collection of messages, otherwise {@literal false}
     */
    public boolean addMessage(Message message) {
        if (message == null) {
            throw new IllegalArgumentException("Message may not be null!");
        }
        return this.messages.add(message);
    }

    /**
     * Returns the checkState to indicate the stock check procedure.
     *
     * @return The checkState
     */
    public String getCheckState() {
        return this.checkState;
    }

    /**
     * Determine whether the Location is considered during allocation.
     *
     * @return {@literal true} when considered in allocation, otherwise {@literal false}
     */
    public boolean isConsideredInAllocation() {
        return this.consideredInAllocation;
    }

    /**
     * Determine whether {@code TransportUnit}s should be counted on this Location or not.
     *
     * @return {@literal true} when counting is active, otherwise {@literal false}
     */
    public boolean isCountingActive() {
        return this.countingActive;
    }

    /**
     * Returns the description of the Location.
     *
     * @return The description text
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Determine whether incoming mode is activated and {@code TransportUnit}s can be put on this Location.
     *
     * @return {@literal true} when incoming mode is activated, otherwise {@literal false}
     */
    public boolean isIncomingActive() {
        return this.incomingActive;
    }

    /**
     * Check whether infeed is blocked and moving {@code TransportUnit}s to here is forbidden.
     *
     * @return {@literal true} is blocked, otherwise {@literal false}
     */
    public boolean isInfeedBlocked() {
        return !this.incomingActive;
    }

    /**
     * Return the date when the Location was updated the last time.
     *
     * @return Timestamp of the last update
     */
    public Date getLastAccess() {
        return this.lastAccess;
    }

    /**
     * Change the date when a TransportUnit was put or left the Location the last time.
     *
     * @param lastAccess The date of change.
     */
    public void setLastAccess(Date lastAccess) {
        this.lastAccess = lastAccess;
    }

    /**
     * Return the {@link LocationGroup} where the Location belongs to.
     *
     * @return The {@link LocationGroup} of the Location
     */
    public LocationGroup getLocationGroup() {
        return this.locationGroup;
    }

    /**
     * Determine whether the Location is part of the parent {@link LocationGroup}s calculation procedure of {@code TransportUnit}s.
     *
     * @return {@literal true} if calculation is activated, otherwise {@literal false}
     */
    public boolean isLocationGroupCountingActive() {
        return this.locationGroupCountingActive;
    }

    /**
     * Returns the locationId (natural key) of the Location.
     *
     * @return The locationId
     */
    public LocationPK getLocationId() {
        return this.locationId;
    }

    /**
     * Returns the type of Location.
     *
     * @return The type
     */
    public LocationType getLocationType() {
        return this.locationType;
    }

    /**
     * Return the maximum allowed weight on the Location.
     *
     * @return The maximum allowed weight
     */
    public BigDecimal getMaximumWeight() {
        return this.maximumWeight;
    }

    /**
     * Returns an unmodifiable Set of {@link Message}s stored for the Location.
     *
     * @return An unmodifiable Set
     */
    public Set<Message> getMessages() {
        return Collections.unmodifiableSet(messages);
    }

    /**
     * Returns the maximum number of {@code TransportUnit}s allowed on the Location.
     *
     * @return The maximum number of {@code TransportUnit}s
     */
    public short getNoMaxTransportUnits() {
        return this.noMaxTransportUnits;
    }

    /**
     * Determine whether outgoing mode is activated and {@code TransportUnit}s can leave this Location.
     *
     * @return {@literal true} when outgoing mode is activated, otherwise {@literal false}
     */
    public boolean isOutgoingActive() {
        return this.outgoingActive;
    }

    /**
     * Check whether outfeed is blocked and moving {@code TransportUnit}s from here is forbidden.
     *
     * @return {@literal true} is blocked, otherwise {@literal false}
     */
    public boolean isOutfeedBlocked() {
        return !this.outgoingActive;
    }

    /**
     * Return the current set plc state.
     *
     * @return the plc state
     */
    public short getPlcState() {
        return this.plcState;
    }

    /**
     * Remove one or more {@link Message}s from this Location.
     *
     * @param msgs An array of {@link Message}s to be removed
     * @return {@literal true} if the {@link Message}s were found and removed, otherwise {@literal false}
     * @throws IllegalArgumentException when messages is {@literal null}
     */
    public boolean removeMessages(Message... msgs) {
        if (msgs == null) {
            throw new IllegalArgumentException("Parameter msgs may not be null!");
        }
        return this.messages.removeAll(Arrays.asList(msgs));
    }

    /**
     * Add this Location to the {@literal locationGroup}. When the argument is {@literal null} an existing {@link LocationGroup} is
     * removed from the Location.
     *
     * @param locationGroup The {@link LocationGroup} to be assigned
     */
    public void setLocationGroup(LocationGroup locationGroup) {
        if (locationGroup != null) {
            this.setLocationGroupCountingActive(locationGroup.isLocationGroupCountingActive());
        }
        this.locationGroup = locationGroup;
    }

    /**
     * {@inheritDoc}
     *
     * Only use the unique natural key for comparison.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Location location = (Location) o;
        return Objects.equals(locationId, location.locationId);
    }

    /**
     * {@inheritDoc}
     *
     * Only use the unique natural key for hashCode calculation.
     */
    @Override
    public int hashCode() {
        return Objects.hash(locationId);
    }

    /**
     * Define whether or not the Location shall be considered in counting {@code TransportUnit}s of the parent {@link LocationGroup}.
     *
     * @param locationGroupCountingActive {@literal true} if considered, otherwise {@literal false}
     */
    public void setLocationGroupCountingActive(boolean locationGroupCountingActive) {
        this.locationGroupCountingActive = locationGroupCountingActive;
    }

    /**
     * Return the {@link LocationPK} as String.
     *
     * @return String locationId
     * @see LocationPK#toString()
     */
    @Override
    public String toString() {
        return this.locationId.toString();
    }
}