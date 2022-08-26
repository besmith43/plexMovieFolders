
cross build --release --target x86_64-unknown-linux-gnu

scp .\target\x86_64-unknown-linux-gnu\release\plex_content_management besmith@10.0.1.2:~/bin/plex_content_management