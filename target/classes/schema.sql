CREATE TABLE IF NOT EXISTS room_cost_config (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    average_daily_room_cost DECIMAL(10, 2) NOT NULL,
    valid_upto_date         DATE           NOT NULL,
    currency                VARCHAR(3)     NOT NULL
);

CREATE TABLE IF NOT EXISTS form_record (
    id                         BIGINT AUTO_INCREMENT PRIMARY KEY,
    form_number                VARCHAR(50)    UNIQUE NOT NULL,
    email_address              VARCHAR(255),
    profile                    VARCHAR(50),
    cover_partner              BOOLEAN        DEFAULT FALSE,
    cover_children             BOOLEAN        DEFAULT FALSE,
    number_of_children         INT            DEFAULT 0,
    age                        VARCHAR(10),
    postcode                   VARCHAR(10),
    optical_needs              VARCHAR(50),
    dental_needs               VARCHAR(50),
    alternative_medicine       VARCHAR(50),
    hospitalisation_preference VARCHAR(50),
    doctor_choice              VARCHAR(50),
    phone_number               VARCHAR(20),
    monthly_premium            DECIMAL(10, 2),
    annual_premium             DECIMAL(10, 2),
    currency                   VARCHAR(3),
    coverage_details           CLOB,
    created_at                 TIMESTAMP      DEFAULT CURRENT_TIMESTAMP
);
