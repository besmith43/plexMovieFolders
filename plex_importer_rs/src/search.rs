extern crate fs_extra;
// use fs_extra::file::*;
use fs_extra::dir::*;
// use fs_extra::error::*;

use inquire::{prompt_u32, Confirm, Select, Text};

use std::path::PathBuf;

use crate::movies::Movie;
use crate::shared::Shared;
use crate::tvshows::TvShow;

pub struct Search {
    pub source: PathBuf,
    pub destination: PathBuf,
}

impl Search {
    pub fn start(&self) {
        let user_choice = &["Movie", "TV Show", "Skip"];

        let mut options = fs_extra::dir::DirOptions::new();
        options.depth = 1;
        let dir_content = fs_extra::dir::get_dir_content2(&self.source, &options).unwrap();

        // have to drop the first as it's the original directory
        let mut directories = dir_content.directories;
        directories.remove(0);

        for directory in directories {
            let ans = Select::new(
                format!("Is {} a TV episode or Movie?", directory).as_str(),
                user_choice.to_vec(),
            )
            .prompt();

            let user_choice_selection = match ans {
                Ok(choice) => {
                    println!("You chose {}", choice);
                    choice
                }
                Err(e) => {
                    eprintln!("ERROR :: {e:?}");
                    continue;
                }
            };

            if user_choice_selection == "Movie" {
                self.new_movie(directory, &options);
            } else if user_choice_selection == "TV Show" {
                self.new_episode(directory, &options);
            } else {
                println!("Skipping");
            }
        }
    }

    fn new_movie(&self, root_dir: String, options: &DirOptions) {
        let contents = fs_extra::dir::get_dir_content2(&root_dir, &options).unwrap();

        let source_file_selection = Select::new("Please select the movie file", contents.files)
            .prompt()
            .unwrap();

        let movie_name_input = Text::new("What is the name of the movie?")
            .prompt()
            .unwrap();

        let movie_year_input = Text::new("What year was the movie released?")
            .prompt()
            .unwrap();

        Movie {
            source: PathBuf::from(source_file_selection),
            destination: PathBuf::from(format!("{}/Movies", self.destination.to_str().unwrap())),
            movie_name: movie_name_input,
            year: movie_year_input.parse().unwrap(),
        }
        .start();
    }

    fn new_episode(&self, root_dir: String, options: &DirOptions) {
        let contents = fs_extra::dir::get_dir_content2(&root_dir, &options).unwrap();

        let source_file_selection = Select::new("Please select the episode file", contents.files)
            .prompt()
            .unwrap();

        let dest_path = format!("{}/TV Shows", self.destination.to_str().unwrap());
        let dest_contents = fs_extra::dir::get_dir_content2(&dest_path, &options).unwrap();

        let series_name_input_index_option = Select::new(
            "What is the name of the TV Show?\nif not found in the list, press Esc",
            dest_contents.directories,
        )
        .prompt();

        // let series_name_input;

        let series_name_input = match series_name_input_index_option {
            Ok(choice) => {
                println!("you chose {}", choice);

                let series_name_input_vec: Vec<&str> = choice.split('/').collect();
                series_name_input_vec[series_name_input_vec.len() - 1].to_string()
            }
            Err(_e) => {
                println!("New Series?");

                let new_series_name = Text::new("What is the name of the New Series?")
                    .prompt()
                    .unwrap();

                new_series_name.to_string()
            }
        };

        println!("{}", series_name_input);

        let season_number = prompt_u32("Season Number: ").unwrap();

        println!("{}", season_number);

        let episode_number = prompt_u32("Episode Number: ").unwrap();

        println!("{}", episode_number);

        let episode_title_confirmation = Confirm::new("Do you know the episode title?")
            .with_default(true)
            .prompt_skippable()
            .unwrap();

        let episode_title_option = match episode_title_confirmation {
            Some(b) => {
                if b {
                    let episode_title = Text::new("Episode Title: ").prompt().unwrap();

                    println!("{}", episode_title);
                    Some("test".to_string())
                } else {
                    None
                }
            }
            None => None,
        };

        TvShow {
            source: PathBuf::from(source_file_selection),
            destination: PathBuf::from(dest_path),
            series_name: series_name_input.to_string(),
            season_number: season_number,
            episode_number: episode_number,
            episode_title: episode_title_option,
        }
        .start();
    }
}
