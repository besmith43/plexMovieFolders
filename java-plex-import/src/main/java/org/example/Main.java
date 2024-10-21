package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        String source = args[0];
        String destinationRoot = "/volume1/Plex/";

        if (source == null) {
            System.out.println("You need to pass in a source path");
            System.exit(1);
        }


    }

    public ContentType CheckType() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Is this a Movie or TV Show");
        String response = scanner.nextLine();

        switch (response) {
            case "TV":
                return ContentType.TVShow;
            case "Movie":
                return ContentType.Movie;
            default:
                return null;
        }
    }

    public void Move(String source, String destination) throws IOException {
        Files.move(Paths.get(source), Paths.get(destination), StandardCopyOption.REPLACE_EXISTING);
    }
}