-- Database Schema for Job Processor Engine
-- Compatible with MySQL, H2, and PostgreSQL
-- Author: Tushar Kapila

-- ---------------------------------------------------------
-- 1. AppParams Table
-- Persistent parameters like numberOfThreads
-- ---------------------------------------------------------
CREATE TABLE AppParams (
    param_name VARCHAR(200) PRIMARY KEY,
    param_value VARCHAR(2000),
    description VARCHAR(1000),
    updated_ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Example Initial Data
INSERT INTO AppParams (param_name, param_value, description) 
VALUES ('numberOfThreads', '5', 'Thread pool size');

-- ---------------------------------------------------------
-- 2. JobProcessor Table
-- Stores processors details
-- ---------------------------------------------------------
CREATE TABLE JobProcessor (
    processor_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_name VARCHAR(500) NOT NULL UNIQUE,
    jar_path VARCHAR(1000) NOT NULL,
    created_ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    active CHAR(1) DEFAULT 'Y'
);

-- ---------------------------------------------------------
-- 3. JobProcessorInstances Table
-- Controls runtime instance count per processor
-- ---------------------------------------------------------
CREATE TABLE JobProcessorInstances (
    instance_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    processor_id BIGINT NOT NULL,
    instance_count INT NOT NULL,
    updated_ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_proc_inst
        FOREIGN KEY (processor_id)
        REFERENCES JobProcessor(processor_id)
);

-- ---------------------------------------------------------
-- 4. InputData Main Table
-- ---------------------------------------------------------
CREATE TABLE InputData (
    input_data_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_name VARCHAR(500),
    processor_class_name VARCHAR(500),
    comment VARCHAR(2000),
    notes VARCHAR(4000),
    job_submitted_datetime TIMESTAMP,
    job_submitted_timezone VARCHAR(100),
    status VARCHAR(50),
    created_ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------
-- 5. InputData Parameters Map Table
-- Key-value flexible storage for input parameters
-- ---------------------------------------------------------
CREATE TABLE InputDataParam (
    param_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    input_data_id BIGINT,
    param_name VARCHAR(500),
    param_type VARCHAR(50),
    string_value VARCHAR(4000),
    number_value DOUBLE,
    date_value TIMESTAMP,
    object_json LONGTEXT, -- Using LONGTEXT instead of CLOB for MySQL compatibility
    CONSTRAINT fk_input_param
        FOREIGN KEY (input_data_id)
        REFERENCES InputData(input_data_id)
);

-- ---------------------------------------------------------
-- 6. InputData Files Table
-- Links to input files on the filesystem
-- ---------------------------------------------------------
CREATE TABLE InputDataFile (
    file_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    input_data_id BIGINT,
    file_name VARCHAR(500),
    file_path VARCHAR(2000),
    file_size BIGINT,
    created_ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_input_file
        FOREIGN KEY (input_data_id)
        REFERENCES InputData(input_data_id)
);

-- ---------------------------------------------------------
-- 7. OutputData Table
-- ---------------------------------------------------------
CREATE TABLE OutputData (
    job_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    input_data_id BIGINT,
    job_name VARCHAR(500),
    processor_class_name VARCHAR(500),
    output_command VARCHAR(2000),
    output_note VARCHAR(4000),
    job_start_datetime TIMESTAMP,
    job_end_datetime TIMESTAMP,
    job_start_timezone VARCHAR(100),
    job_end_timezone VARCHAR(100),
    main_error_reason VARCHAR(4000),
    status VARCHAR(50),
    created_ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------
-- 8. OutputDataParam Table
-- ---------------------------------------------------------
CREATE TABLE OutputDataParam (
    param_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id BIGINT,
    param_name VARCHAR(500),
    param_type VARCHAR(50),
    string_value VARCHAR(4000),
    number_value DOUBLE,
    date_value TIMESTAMP,
    object_json LONGTEXT, -- Using LONGTEXT for MySQL
    CONSTRAINT fk_out_param
        FOREIGN KEY (job_id)
        REFERENCES OutputData(job_id)
);

-- ---------------------------------------------------------
-- 9. OutputDataFile Table
-- ---------------------------------------------------------
CREATE TABLE OutputDataFile (
    file_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id BIGINT,
    file_name VARCHAR(500),
    file_path VARCHAR(2000),
    file_size BIGINT,
    created_ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_out_file
        FOREIGN KEY (job_id)
        REFERENCES OutputData(job_id)
);

-- ---------------------------------------------------------
-- 10. JobError Table
-- ---------------------------------------------------------
CREATE TABLE JobError (
    error_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id BIGINT,
    reason_code VARCHAR(200),
    reason_string VARCHAR(4000),
    created_ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_job_error
        FOREIGN KEY (job_id)
        REFERENCES OutputData(job_id)
);
