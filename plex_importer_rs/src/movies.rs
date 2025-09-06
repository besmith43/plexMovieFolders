use regex::Regex;
// use std::path::PathBuf;

use indicatif::{ProgressBar, ProgressStyle};
use std::fs::{self, File};
use std::io::{self, BufReader, BufWriter, Read, Write};
use std::path::{Path, PathBuf};

use crate::shared::Shared;

pub struct Movie {
    pub source: PathBuf,
    pub destination: PathBuf,
    pub year: u32,
    pub movie_name: String,
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

        clearscreen::clear().unwrap();
    }

    fn build_destination_path(&mut self) {
        let movie_dir = format!(
            "{}/{} ({})",
            &self.destination.to_str().unwrap(),
            &self.movie_name,
            &self.year
        );

        println!("Make Movie home directory at {}", movie_dir);
        fs_extra::dir::create_all(&movie_dir, false).unwrap();

        self.destination = PathBuf::from(&movie_dir);
    }

    fn build_filename(&mut self) {
        let filename = format!(
            "{}/{} ({}).{}",
            &self.destination.to_str().unwrap(),
            &self.movie_name,
            &self.year,
            &self.source.extension().unwrap().to_str().unwrap()
        );

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
        // println!(
        // "Moving file from\n\t{}\nto\n\t{}",
        // &self.source.to_str().unwrap(),
        // &self.destination.to_str().unwrap()
        // );
        // let c_options = fs_extra::file::CopyOptions::new();
        // let _ = fs_extra::file::move_file(&self.source, &self.destination, &c_options);

        // 1. Get the size of the source file to set the progress bar length.
        let file_size = fs::metadata(&self.source).unwrap().len();

        // 2. Set up the progress bar.
        let pb = ProgressBar::new(file_size);
        pb.set_style(ProgressStyle::default_bar()
        .template("{spinner:.green} [{elapsed_precise}] [{bar:40.cyan/blue}] {bytes}/{total_bytes} ({eta})")
        .unwrap()
        .progress_chars("#>-"));

        // 3. Open the source file for reading.
        let mut source_file = BufReader::new(File::open(&self.source).unwrap());

        // 4. Create and open the destination file for writing.
        let mut dest_file = BufWriter::new(File::create(&self.destination).unwrap());

        // 5. Copy the data in chunks and update the progress bar.
        let mut buffer = [0; 8192]; // 8 KB buffer for copying chunks
        loop {
            let bytes_read = source_file.read(&mut buffer).unwrap();
            if bytes_read == 0 {
                break;
            }
            dest_file.write_all(&buffer[..bytes_read]).unwrap();
            pb.inc(bytes_read as u64);
        }

        // 6. Finish the progress bar and clear it from the terminal.
        pb.finish_and_clear();

        // 7. Close the files and remove the original source file.
        // The File and BufWriter destructors handle closing, but we must
        // explicitly remove the original file to complete the "move" operation.
        fs::remove_file(&self.source).unwrap();

        // Ok(())
    }

    fn remove_root_dir(&self) {
        println!(
            "Deleting parent folder {}",
            self.source.parent().unwrap().to_str().unwrap()
        );
        let _ = fs_extra::dir::remove(self.source.parent().unwrap());
    }
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
}
