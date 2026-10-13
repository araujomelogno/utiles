`silence.m4a` is a 959-byte, mono 16 kHz AAC recording of one second of generated
silence. It contains no recorded speech or third-party audio. Its final `mdat` box
uses the valid size-zero (to EOF) header that jaudiotagger 2.0.1 rejects.

The seed was generated once during development using:

```sh
ffmpeg -f lavfi -i anullsrc=r=16000:cl=mono -t 1 -c:a aac -movflags +faststart silence.m4a
```

The final `mdat` box's four-byte size field was then set to zero. The tests load
this committed file directly. `JavaAudioFixtures` produces the extended-size
variant and a 1703.04-second recording by remuxing real AAC samples in Java.
Neither tests nor application execution require FFmpeg or ffprobe.
