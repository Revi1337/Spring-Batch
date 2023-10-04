package com.example.springbatchtutorial.job.FileDataReadWrite.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.Year;

@Data
public class PlayerYears implements Serializable {

    private String ID;
    private String lastName;
    private String firstName;
    private String position;
    private int birthYear;
    private int debutYear;
    private int yearsExperience;

    public PlayerYears(Player player) {
        this.ID = player.getID();
        this.lastName = player.getLastName();
        this.firstName = player.getFirstName();
        this.position = player.getPosition();
        this.birthYear = player.getBirthYear();
        this.debutYear = player.getDebutYear();
        this.yearsExperience = Year.now().getValue() - player.getDebutYear();
    }
}
