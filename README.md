# Skip times

Frame-checked intro, recap, credits and preview times, one JSON file per show, read by the patches.

`times/<imdb>.json`: `{"v":1,"s":{"<season>":{"<episode>":[{"t":"intro","a":71196,"b":161036,"d":1511977}]}}}`

`t` is intro, recap, credits or preview. `a` and `b` are start and end in milliseconds. `d` is the length in milliseconds of the file the times were measured on. An entry applies only when the viewer's file length is within 2 seconds of `d`.
