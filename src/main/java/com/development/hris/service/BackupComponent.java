package com.development.hris.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BackupComponent {

    private final JdbcTemplate jdbcTemplate;

    @Scheduled(fixedRate = 86400000)
    public void runTask() {
        String backupName = "dump_" + new Date().toString().trim().replace(":", "").replace(" ", "") + ".sql";
        log.info("Backup created at: " + new Date().toString());

        // Execute the SCRIPT TO 'dump.sql' command
        jdbcTemplate.execute("SCRIPT TO '"+ backupName +"'");
    }
}
