// extern crate fs_extra;
// use fs_extra::file::*;
// use fs_extra::dir::*;
// use fs_extra::error::*;

// use inquire::{Confirm, Select, Text, prompt_u32};

// use regex::Regex;
use std::path::PathBuf;

use clap::Parser;


// importing my code
mod movies;
pub use crate::movies::Movie;

mod tvshows;
pub use crate::tvshows::TvShow;

mod shared;
pub use crate::shared::Shared;

mod search;
pub use self::search::Search; // either crate or self work for this imports

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
    }.start();
}





