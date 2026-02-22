-- Seed data for Job Processor Engine
-- Author: Tushar Kapila

INSERT INTO AppParams (param_name, param_value, description) VALUES
    ('numberOfThreads', '5', 'Thread pool size');

INSERT INTO AppParams (param_name, param_value, description) VALUES
    ('processorJarDirectory', './processors', 'Directory for processor JARs');

INSERT INTO AppParams (param_name, param_value, description) VALUES
    ('inputFileDirectory', './inputFiles', 'Directory for input files');

INSERT INTO AppParams (param_name, param_value, description) VALUES
    ('outputFileDirectory', './outputFiles', 'Directory for output files');
