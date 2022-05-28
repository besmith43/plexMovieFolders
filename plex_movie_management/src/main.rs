use structopt::StructOpt;
use std::path::{Path, PathBuf};

#[derive(Debug, StructOpt)]
struct Opt {
    #[structopt(short, long, parse(from_os_str), default_value = "/volume1/docker/sabnzbd/Downloads/complete/")]
    source: PathBuf,

    #[structopt(short, long, parse(from_os_str), default_value = "/volume1/Plex/Movies/")]
    destination: PathBuf,

    #[structopt(short, long)]
    movie: String,

    #[structopt(short, long)]
    year: u32,

    #[structopt(short, long)]
    verbose: bool,

    #[structopt(short, long)]
    what_if: bool
}

fn main() {
    let opt = Opt::from_args();

    println!("{:?}", opt);
}
