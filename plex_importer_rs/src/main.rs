extern crate fs_extra;
// use fs_extra::file::*;
use fs_extra::dir::*;
// use fs_extra::error::*;

use inquire::{Confirm, Select, Text, prompt_u32};

use regex::Regex;
use std::path::PathBuf;

use clap::Parser;

#[derive(Parser, Debug)]
#[command(version, about, long_about = None)]
struct Args {
    #[arg(short, long, default_value = "/volume1/docker/sabnzbd/Downloads/complete/new-uploads")]
    source_root: PathBuf,
    #[arg(short, long, default_value = "/volume1/Plex")]
    destination_root: PathBuf,
    #[arg(short, long)] // defaults to false
    verbose: bool,
}


fn main() {
    let args = Args::parse();
    
    eprintln!("Source: {}", args.source_root.to_str().unwrap());
    eprintln!("Dest: {}", args.destination_root.to_str().unwrap());
    eprintln!("Verbose: {:?}", args.verbose);


    Search {
        source: args.source_root,
        destination: args.destination_root,
        debug: false,
    }.start();
}

//=============================================================================

struct Movie {
    source: PathBuf,
    destination: PathBuf,
    year: u32,
    movie_name: String,
}

impl Movie {
    fn check_year(&self) -> bool {
        let re = Regex::new(r"\d{4}$").unwrap();

        re.is_match(&self.year.to_string())
    }
}

impl Shared for Movie {
    fn start(&mut self) {
        if !self.check_year() {
            eprintln!("the value for the year variable given isn't an appropriate year");
            std::process::exit(1);
        }

        self.build_destination_path();

        self.build_filename();

        self.check_for_duplicate();

        self.move_operation();

        self.remove_root_dir();
    }

    fn build_destination_path(&mut self) {
        let movie_dir = format!("{}/{} ({})",
                                &self.destination.to_str().unwrap(),
                                &self.movie_name,
                                &self.year);

        println!("Make Movie home directory at {}", movie_dir);
        fs_extra::dir::create_all(&movie_dir, false).unwrap();
    
        self.destination = PathBuf::from(&movie_dir);
    }

    fn build_filename(&mut self) {
        let filename = format!("{}/{} ({}).{}",
                                &self.destination.to_str().unwrap(),
                                &self.movie_name,
                                &self.year,
                                &self.source.extension().unwrap().to_str().unwrap());

        println!("filename generated {}", &filename);

        self.destination = PathBuf::from(filename);
    }

    fn check_for_duplicate(&self) {
        if self.destination.exists() {
            println!("quitting application");
            std::process::exit(0);
        }
    }

    fn move_operation(&self) {
        println!("Moving file from {} to {}",
                &self.source.to_str().unwrap(),
                &self.destination.to_str().unwrap());
        let c_options = fs_extra::file::CopyOptions::new();
        let _ = fs_extra::file::move_file(&self.source, &self.destination, &c_options);
    }

    fn remove_root_dir(&self) {
        println!("Deleting parent folder {}",
                self.source.parent().unwrap().to_str().unwrap());
        fs_extra::dir::remove(self.source.parent().unwrap());
    }
}

//========================================================

struct TvShow {
    source: PathBuf,
    destination: PathBuf,
    series_name: String,
    season_number: u32,
    episode_number: u32,
    episode_title: Option<String>,
}

impl TvShow {
    // this needs to take the season number and turn it into a string that looks like xx
    fn convert_number(&self, number: u32) -> String {
        if number < 10 {
            format!("0{}", number)
        } else {
            format!("{}", number)
        }
    }
}

impl Shared for TvShow {
    fn start(&mut self) {
        self.build_destination_path();

        self.build_filename();

        self.check_for_duplicate();

        self.move_operation();

        self.remove_root_dir();
    }

    // use this to overwrite the dest_dir with the new root dir created
    fn build_destination_path(&mut self) {
        let root_dir = format!("{}/{}/Season {}", 
                                self.destination.to_str().unwrap(), 
                                &self.series_name, 
                                self.convert_number(self.season_number.clone()));

        println!("Make TV Show directory at {}", root_dir);
        let _ = fs_extra::dir::create_all(&root_dir, false);

        self.destination = PathBuf::from(root_dir);
    }

    // this depends on build_destination_path
    // also overwrites the dest_dir
    fn build_filename(&mut self) {
        let filename: String;

        if self.episode_title.is_some() {
            filename = format!("{}/{} - s{}e{} - {}.{}",
                                &self.destination.to_str().unwrap(),
                                &self.series_name,
                                self.convert_number(self.season_number),
                                self.convert_number(self.episode_number),
                                self.episode_title.as_ref().unwrap(),
                                &self.source.extension().unwrap().to_str().unwrap());
        } else {
            filename = format!("{}/{} - s{}e{}.{}",
                                &self.destination.to_str().unwrap(),
                                &self.series_name,
                                self.convert_number(self.season_number.clone()),
                                self.convert_number(self.episode_number.clone()),
                                &self.source.extension().unwrap().to_str().unwrap());
        }

        println!("filename generated {}", &filename);

        self.destination = PathBuf::from(filename);
    }

    fn check_for_duplicate(&self) {
        if self.destination.exists() {
            println!("quitting application");
            std::process::exit(0);
        }
    }

    fn move_operation(&self) {
            println!("Moving file from {} to {}",
                     &self.source.to_str().unwrap(),
                     &self.destination.to_str().unwrap());
            let c_options = fs_extra::file::CopyOptions::new();
            fs_extra::file::move_file(&self.source, &self.destination, &c_options);
    }

    fn remove_root_dir(&self) {
        println!("Deleting parent folder {}",
                self.source.parent().unwrap().to_str().unwrap());
        fs_extra::dir::remove(self.source.parent().unwrap());
    }
}

//=====================================================================

struct Search {
    source: PathBuf,
    destination: PathBuf,
    debug: bool,
}

impl Search {
    fn start(&self) {
        let user_choice = &[
            "Movie",
            "TV Show",
            "Skip",
        ];

        let mut options = fs_extra::dir::DirOptions::new();
        options.depth = 1;
        let dir_content = fs_extra::dir::get_dir_content2(&self.source, &options).unwrap();

        // have to drop the first as it's the original directory
        let mut directories = dir_content.directories;
        directories.remove(0);

        for directory in directories {
            let ans = Select::new(format!("Is {} a TV episode or Movie?", directory).as_str(), user_choice.to_vec()).prompt();

            let user_choice_selection = match ans {
                Ok(choice) => {
                    println!("You chose {}", choice);
                    choice
                },
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

        let source_file_selection = Select::new("Please select the movie file", contents.files).prompt().unwrap();

        let movie_name_input = Text::new("What is the name of the movie?").prompt().unwrap();

        let movie_year_input = Text::new("What year was the movie released?").prompt().unwrap();

        Movie {
            source: PathBuf::from(source_file_selection),
            destination: PathBuf::from(format!("{}/Movies", self.destination.to_str().unwrap())),
            movie_name: movie_name_input,
            year: movie_year_input.parse().unwrap(),
        }.start();
    }

    fn new_episode(&self, root_dir: String, options: &DirOptions) {
        let contents = fs_extra::dir::get_dir_content2(&root_dir, &options).unwrap();

        let source_file_selection = Select::new("Please select the episode file", contents.files).prompt().unwrap();

        let dest_path = format!("{}/TV Shows", self.destination.to_str().unwrap());
        let dest_contents = fs_extra::dir::get_dir_content2(&dest_path, &options).unwrap();

        let series_name_input_index_option = Select::new("What is the name of the TV Show?\nif not found in the list, press Esc", dest_contents.directories).prompt();

        // let series_name_input;

        let series_name_input = match series_name_input_index_option {
            Ok(choice) => {
                println!("you chose {}", choice);

                let series_name_input_vec: Vec<&str> = choice.split('/').collect();
                series_name_input_vec[series_name_input_vec.len()-1].to_string()
            },
            Err(_e) => {
                println!("New Series?");

                let new_series_name = Text::new("What is the name of the New Series?").prompt().unwrap();

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
            },
            None => None,
        };

        TvShow {
            source: PathBuf::from(source_file_selection),
            destination: PathBuf::from(dest_path),
            series_name: series_name_input.to_string(),
            season_number: season_number,
            episode_number: episode_number,
            episode_title: episode_title_option,
        }.start();
    }
}

//=======================================================================

trait Shared {
    fn start(&mut self);

    fn build_destination_path(&mut self);

    fn build_filename(&mut self);

    fn check_for_duplicate(&self);

    fn move_operation(&self);

    fn remove_root_dir(&self);
}


#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_movie() {
        ()
    }

    #[test]
    fn test_movie_year() {
        let mut test_movie = Movie {
            source: PathBuf::from(""),
            destination: PathBuf::from(""),
            movie_name: "".to_string(),
            year: 1995,
        };

        assert!(test_movie.check_year());

        test_movie.year = 5;
        
        assert!(!test_movie.check_year());
    }


    #[test]
    fn test_tv_show() {
        ()
    }


    #[test]
    fn test_tv_convert() {
        let test_tv_show = TvShow {
            source: PathBuf::from(""),
            destination: PathBuf::from(""),
            series_name: "".to_string(),
            season_number: 1,
            episode_number: 10,
            episode_title: Some("test".to_string()),
        };

        assert_eq!(test_tv_show.convert_number(2), "02");
        assert_ne!(test_tv_show.convert_number(5), "5");
        assert_eq!(test_tv_show.convert_number(10), "10");
    }
}
