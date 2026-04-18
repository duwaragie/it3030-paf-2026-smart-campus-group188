-- V1__Create_Bookings_Table.sql
-- Create bookings table for booking management module

CREATE TABLE IF NOT EXISTS bookings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    user_name VARCHAR(255) NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    resource_id BIGINT NOT NULL,
    resource_name VARCHAR(255) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    purpose VARCHAR(500) NOT NULL,
    expected_attendees INTEGER,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    rejection_reason VARCHAR(1000),
    approved_by_id BIGINT,
    approved_by_name VARCHAR(255),
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT chk_time CHECK (start_time < end_time)
);

-- Create indexes for better query performance
CREATE INDEX idx_bookings_user_id ON bookings(user_id);
CREATE INDEX idx_bookings_resource_id ON bookings(resource_id);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_bookings_start_time ON bookings(start_time);
CREATE INDEX idx_bookings_end_time ON bookings(end_time);
CREATE INDEX idx_bookings_resource_time ON bookings(resource_id, start_time, end_time);

-- Add comment to table
COMMENT ON TABLE bookings IS 'Stores booking requests for resources (rooms, facilities)';
COMMENT ON COLUMN bookings.status IS 'Booking status: PENDING (awaiting approval), APPROVED (approved by admin), REJECTED (rejected by admin), CANCELLED (cancelled by user)';
COMMENT ON COLUMN bookings.requested_at IS 'Timestamp when booking was requested - immutable';
COMMENT ON COLUMN bookings.updated_at IS 'Timestamp when booking was last updated';
