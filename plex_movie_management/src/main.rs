extern crate fs_extra;
use fs_extra::file::*;
use fs_extra::dir::*;
use fs_extra::error::*;

use structopt::StructOpt;
use regex::Regex;
use std::borrow::Borrow;
use std::path::{Path, PathBuf};
use dialoguer::{theme::ColorfulTheme, Input, Confirm, Select, FuzzySelect};

/// Blake Smith <besmith43@gmail.com>
///
/// plex_content_management is a cli application with the goal of renaming and organizing new content
/// in my plex directories more automated
#[derive(Debug, StructOpt)]
#[structopt(name = "plex_movie_management")]
enum Opt {
    Movie {
        /// original movie file
        #[structopt(short = "i", long, parse(from_os_str))]//, default_value = "/volume1/docker/sabnzbd/Downloads/complete/")]
        input: PathBuf,

        /// the movies directory of plex
        #[structopt(short = "o", long, parse(from_os_str), default_value = "/volume1/Plex/Movies")]
        output: PathBuf,

        /// Movie Title
        #[structopt(short = "m", long)]
        movie: String,

        /// year of the movie's release
        #[structopt(short = "y", long)]
        year: u32,

        /// Will only print to the console the action that would have been taken
        #[structopt(long)]
        what_if: bool,

        /// Debug Mode
        #[structopt(long)]
        debug: bool,
    },
    TV {
        /// original movie file
        #[structopt(short = "i", long, parse(from_os_str))]
        input: PathBuf,

        /// the movies directory of plex
        #[structopt(short = "o", long, parse(from_os_str), default_value = "/volume1/Plex/TV Shows")]
        output: PathBuf,

        /// series name
        #[structopt(short = "s", long)]
        series: String,

        /// season number
        #[structopt(short = "n", long, default_value = "1")]
        season: u32,

        /// episode number
        #[structopt(short = "e", long, default_value = "1")]
        episode: u32,

        /// episode title
        #[structopt(short = "t", long)]
        title: Option<String>,

        /// Will only print to the console the action that would have been taken
        #[structopt(long)]
        what_if: bool,

        /// Debug Mode
        #[structopt(short = "d", long)]
        debug: bool, 
    },
    Search {
        /// directory to search
        #[structopt(short = "i", long, parse(from_os_str), default_value = "/volume1/docker/sabnzbd/Downloads/complete")]
        input: PathBuf,

        /// plex directory
        #[structopt(short = "o", long, parse(from_os_str), default_value = "/volume1/Plex")]
        output: PathBuf,

        /// Debug Mode
        #[structopt(long)]
        debug: bool, 
    },
}

// see link for why variables of the same name don't have to be paired with a colon
// https://www.reddit.com/r/rust/comments/pai1o8/media_field_init_shorthand_in_rust/

fn main() {
    match Opt::from_args() {
        Opt::Movie { input, output, movie, year, what_if, debug } => {
            Movie {
                source: input,
                destination: output,
                year,
                movie_name: movie,
                what_if,
                debug,
            }.start();
        },
        Opt::TV { input, output, series, season, episode, title, what_if, debug } => {
            TvShow {
                source: input,
                destination: output,
                series_name: series,
                season_number: season,
                episode_number: episode,
                episode_title: title,
                what_if,
                debug,
            }.start();
        },
        Opt::Search { input, output, debug } => {
            Search {
                source: input,
                destination: output,
                debug,
            }.start();
        },
    }
}

//=============================================================================

struct Movie {
    source: PathBuf,
    destination: PathBuf,
    year: u32,
    movie_name: String,
    what_if: bool,
    debug: bool,
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

        if self.what_if {
            println!("Make Movie home directory at {}", movie_dir);
        } else {
            self.debug_println(format!("Make Movie home directory at {}", movie_dir));
            fs_extra::dir::create_all(&movie_dir, false).unwrap();
        }
    
        self.destination = PathBuf::from(&movie_dir);
    }

    fn build_filename(&mut self) {
        let filename = format!("{}/{} ({}).{}",
                                &self.destination.to_str().unwrap(),
                                &self.movie_name,
                                &self.year,
                                &self.source.extension().unwrap().to_str().unwrap());

        self.debug_println(format!("filename generated {}", &filename));

        self.destination = PathBuf::from(filename);
    }

    fn check_for_duplicate(&self) {
        if self.destination.exists() {
            if Confirm::with_theme(&ColorfulTheme::default())
                .with_prompt(format!("the file, {}, already exists\nwould you like to overwrite it?",
                        &self.destination.to_str().unwrap()))
                .default(true)
                .interact()
                .unwrap() {
                self.debug_println("quitting application".to_string());
                std::process::exit(0);
            }
        }
    }

    fn move_operation(&self) {
        if self.what_if {
            println!("Moving file from {} to {}",
                     &self.source.to_str().unwrap(),
                     &self.destination.to_str().unwrap());
        } else {
            self.debug_println(format!("Moving file from {} to {}",
                     &self.source.to_str().unwrap(),
                     &self.destination.to_str().unwrap()));
            let c_options = fs_extra::file::CopyOptions::new();
            fs_extra::file::move_file(&self.source, &self.destination, &c_options);
        }
    }

    fn remove_root_dir(&self) {
        if self.what_if {
            println!("Deleting parent folder {}",
                     self.source.parent().unwrap().to_str().unwrap());
        } else {
            self.debug_println(format!("Deleting parent folder {}",
                    self.source.parent().unwrap().to_str().unwrap()));
            fs_extra::dir::remove(self.source.parent().unwrap());
        }
    }

    fn debug_println(&self, message: String) {
        if self.debug {
            println!("{}", message);
        }
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
    what_if: bool,
    debug: bool,
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

        if self.what_if {
            println!("Make TV Show directory at {}", root_dir);
        } else {
            self.debug_println(format!("Make TV Show directory at {}", root_dir));
            fs_extra::dir::create_all(&root_dir, false);
        }

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

        self.debug_println(format!("filename generated {}", &filename));

        self.destination = PathBuf::from(filename);
    }

    fn check_for_duplicate(&self) {
        if self.destination.exists() {
            if Confirm::with_theme(&ColorfulTheme::default())
                .with_prompt(format!("the file, {}, already exists\nwould you like to overwrite it?",
                        &self.destination.to_str().unwrap()))
                .default(true)
                .interact()
                .unwrap() {
                self.debug_println("quitting application".to_string());
                std::process::exit(0);
            }
        }
    }

    fn move_operation(&self) {
        if self.what_if {
            println!("Moving file from {} to {}",
                     &self.source.to_str().unwrap(),
                     &self.destination.to_str().unwrap());
        } else {
            self.debug_println(format!("Moving file from {} to {}",
                     &self.source.to_str().unwrap(),
                     &self.destination.to_str().unwrap()));
            let c_options = fs_extra::file::CopyOptions::new();
            fs_extra::file::move_file(&self.source, &self.destination, &c_options);
        }
    }

    fn remove_root_dir(&self) {
        if self.what_if {
            println!("Deleting parent folder {}",
                     self.source.parent().unwrap().to_str().unwrap());
        } else {
            self.debug_println(format!("Deleting parent folder {}",
                    self.source.parent().unwrap().to_str().unwrap()));
            fs_extra::dir::remove(self.source.parent().unwrap());
        }
    }

    fn debug_println(&self, message: String) {
        if self.debug {
            println!("{}", message);
        }
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
            let user_choice_selection = Select::with_theme(&ColorfulTheme::default())
                .with_prompt(format!("Is {} a TV episode or Movie", directory))
                .items(user_choice)
                .interact()
                .unwrap();

            if user_choice_selection == 0 {
                self.new_movie(directory, &options);
            } else if user_choice_selection == 1 {
                self.new_episode(directory, &options);
            } else {
                println!("Skipping");
            }
        }
    }

    fn new_movie(&self, root_dir: String, options: &DirOptions) {
        let contents = fs_extra::dir::get_dir_content2(&root_dir, &options).unwrap();

        let source_file_selection = Select::with_theme(&ColorfulTheme::default())
            .with_prompt("Please select the movie file")
            .default(0)
            .items(&contents.files)
            .interact()
            .unwrap();

        let movie_name_input: String = Input::with_theme(&ColorfulTheme::default())
            .with_prompt("What is the name of the movie?")
            .interact_text()
            .unwrap();

        let movie_year_input: String = Input::with_theme(&ColorfulTheme::default())
            .with_prompt("What year was the movie released?")
            .interact_text()
            .unwrap();

        Movie {
            source: PathBuf::from(&contents.files[source_file_selection]),
            destination: PathBuf::from(format!("{}/Movies", self.destination.to_str().unwrap())),
            movie_name: movie_name_input,
            year: movie_year_input.parse().unwrap(),
            what_if: false,
            debug: self.debug,
        }.start();
    }

    fn new_episode(&self, root_dir: String, options: &DirOptions) {
        let contents = fs_extra::dir::get_dir_content2(&root_dir, &options).unwrap();

        let source_file_selection = Select::with_theme(&ColorfulTheme::default())
            .with_prompt("Please select the episode file")
            .default(0)
            .items(&contents.files)
            .interact()
            .unwrap();

        let dest_path = format!("{}/TV Shows", self.destination.to_str().unwrap());
        let dest_contents = fs_extra::dir::get_dir_content2(&dest_path, &options).unwrap();

        // swap this to fuzzy select with a items option of the current folders of /Plex/TV Shows/*
        let series_name_input_index_option = FuzzySelect::with_theme(&ColorfulTheme::default())
            .items(&dest_contents.directories[..])
            .with_prompt("What is the name of the TV Show?\nif not found in the list, press Esc")
            .interact_opt()
            .unwrap();

        let series_name_input;

        match series_name_input_index_option {
            Some(index) => {
                let series_name_input_path = &dest_contents.directories[index];
                let series_name_input_vec: Vec<&str> = series_name_input_path.split('/').collect();
                series_name_input = series_name_input_vec[series_name_input_vec.len()-1];
            },
            None => {
                let temp_input: String = Input::with_theme(&ColorfulTheme::default())
                    .with_prompt("New Series Name?")
                    .interact_text()
                    .unwrap();

                series_name_input = Box::leak(temp_input.into_boxed_str());
            }
        }

        println!("{}", &root_dir);

        let season_number_input: String = Input::with_theme(&ColorfulTheme::default())
            .with_prompt("Which season is this episode from?")
            .interact_text()
            .unwrap();

        let episode_number_input: String = Input::with_theme(&ColorfulTheme::default())
            .with_prompt("What is the number of the episode?")
            .interact_text()
            .unwrap();

        let episode_title_option: Option<String>;
        if Confirm::with_theme(&ColorfulTheme::default())
            .with_prompt("Do you know the episode title?")
            .default(true)
            .interact()
            .unwrap() {
            let title_temp: String = Input::with_theme(&ColorfulTheme::default())
                    .with_prompt("What is the episode title?")
                    .interact()
                    .unwrap();

            episode_title_option = Some(title_temp);
        } else {
            episode_title_option = None;
        }

        TvShow {
            source: PathBuf::from(&contents.files[source_file_selection]),
            destination: PathBuf::from(dest_path),
            series_name: series_name_input.to_string(),
            season_number: season_number_input.parse().unwrap(),
            episode_number: episode_number_input.parse().unwrap(),
            episode_title: episode_title_option,
            what_if: false,
            debug: self.debug,
        }.start();
    }
}
/*
impl Shared for Search {
    fn start(&mut self) {

    }

    fn build_destination_path(&mut self) {

    }

    fn build_filename(&mut self) {

    }

    fn check_for_duplicate(&self) {

    }

    fn move_operation(&self) {

    }

    fn remove_root_dir(&self) {

    }

    fn debug_println(&self, message: String) {
        if self.debug {
            println!("{}", message);
        }
    }
}
*/
//=======================================================================

trait Shared {
    fn debug_println(&self, message: String);

    fn start(&mut self);

    fn build_destination_path(&mut self);

    fn build_filename(&mut self);

    fn check_for_duplicate(&self);

    fn move_operation(&self);

    fn remove_root_dir(&self);
}

