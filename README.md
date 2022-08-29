# Scontrino Proxy

A simple TCP/IP proxy and sniffer written in Kotlin.

`scontrino-proxy` transcribes all network interactions between a proxied server and its clients for subsequent off-line 
analysis. This is useful for analyzing and reverse engineering undocumented protocols.

## Command-line Invocation

Under the `dist` directory there are










 *nix shell and Windows batch scripts to run the proxy.

A sample invocation proxying a Postgres server would look like:

```
$ scontrino-proxy \
  --host=localhost --remote-port=5432 --local-port=2345 \
  --dump-file=./postgres-interactions.tsv --buffer-size=32768
```

where:

- `--host` names the proxied remote host. Defaults to `localhost` 
- `--remote-port` specifies the port to be proxied on the remote host. Defaults to `8080` 
- `--local-port` the local port on which clients will be serviced. Defaults to `8081`
- `--dump-file` names the tab-separated file where all interactions will be logged (see below). Defaults to `.
  /proxied-interactions.tsv`
- `--buffer-size` specifies the chunk size to read (and dump) observed interactions. Defaults to `4096`

## The Dump File

Interactions between clients and server are transcribed verbatim and also collected into a tab-separated UTF-8 file 
containing:

- The interaction UUID
- The client IP address
- The interaction timestamp (as milliseconds since the epoch)
- The interaction type (client `REQUEST` or server `RESPONSE`)
- The interaction's byte array payload (encoded in base 64)

Given the above Postgres proxy invocation, the following `psql` session:

```
$ psql -h localhost -p 2345 -U postgres
Password for user postgres:
psql (14.5, server 14.4 (Debian 14.4-1.pgdg110+1))
Type "help" for help.

postgres=# select count(*) from information_schema.tables;
 count
-------
   203
(1 row)

postgres=# \q
$
```

would yield the following first few lines in the dump file:

```
e07ffb10-fdae-4189-9157-34f724d7561e	localhost/127.0.0.1	1661791521670	REQUEST	AAAACATSFi8=
e07ffb10-fdae-4189-9157-34f724d7561e	localhost/127.0.0.1	1661791521674	RESPONSE	Tg==
e07ffb10-fdae-4189-9157-34f724d7561e	localhost/127.0.0.1	1661791521674	REQUEST	AAAAVAADAAB1c2VyAHBvc3RncmVzAGRhdGFiYXNlAHBvc3RncmVzAGFwcGxpY2F0aW9uX25hbWUAcHNxbABjbGllbnRfZW5jb2RpbmcAVVRGOAAA
e07ffb10-fdae-4189-9157-34f724d7561e	localhost/127.0.0.1	1661791521678	RESPONSE	UgAAABcAAAAKU0NSQU0tU0hBLTI1NgAA
ce9fc2b0-a0b9-4ab3-beaf-f8ea34d2f82c	localhost/127.0.0.1	1661791524050	REQUEST	AAAACATSFi8=
ce9fc2b0-a0b9-4ab3-beaf-f8ea34d2f82c	localhost/127.0.0.1	1661791524055	RESPONSE	Tg==
```

with the following hex dump:

``` 
REQUEST
00000000: 0000 0008 04d2 162f                      ......./

REQUEST
00000000: 0000 0054 0003 0000 7573 6572 0070 6f73  ...T....user.pos
00000010: 7467 7265 7300 6461 7461 6261 7365 0070  tgres.database.p
00000020: 6f73 7467 7265 7300 6170 706c 6963 6174  ostgres.applicat
00000030: 696f 6e5f 6e61 6d65 0070 7371 6c00 636c  ion_name.psql.cl
00000040: 6965 6e74 5f65 6e63 6f64 696e 6700 5554  ient_encoding.UT
00000050: 4638 0000                                F8..

RESPONSE
00000000: 4e                                       N

RESPONSE
00000000: 5200 0000 1700 0000 0a53 4352 414d 2d53  R........SCRAM-S
00000010: 4841 2d32 3536 0000                      HA-256..

REQUEST
00000000: 0000 0008 04d2 162f                      ......./

RESPONSE
00000000: 4e                                       N
```

## What's Next?

The next step in the roadmap is to allow the proxy to recognize certain requests and re-route them to alternate
services (possibly after request preprocessing) while still forwarding unrecognized traffic to the proxied server.

This could enable incremental migration of server responsibilities in a way reminiscent of 
[Hans Moravec](https://en.wikipedia.org/wiki/Hans_Moravec)'s 1989 vision outlined in his
["After Life"](https://frc.ri.cmu.edu/~hpm/project.archive/robot.papers/1989/Afterlife.html):

> You've just been wheeled into the operating room. A robot brain surgeon
is in attendance, a computer waits nearby. Your skull, but not your
brain, is anesthetized. You are fully conscious. The robot surgeon opens
your brain case and places a hand on the brain's surface. This unusual
hand bristles with microscopic machinery, and a cable connects it to the
computer at your side. Instruments in the hand scan the first few
millimeters of brain surface. These measurements, and a comprehensive
understanding of human neural architecture, allow the surgeon to write a
program that models the behavior of the uppermost layer of the scanned
brain tissue. This program is installed in a small portion of the
waiting computer and activated. Electrodes in the hand supply the
simulation with the appropriate inputs from your brain, and can inject
signals from the simulation. You and the surgeon compare the signals it
produces with the original ones. They flash by very fast, but any
discrepancies are highlighted on a display screen. The surgeon
fine-tunes the simulation until the correspondence is nearly perfect. As
soon as you are satisfied, the simulation output is activated. The brain
layer is now impotent—it receives inputs and reacts as before but its
output is ignored. Microscopic manipulators on the hand's surface excise
this superfluous tissue and pass them to an aspirator, where they are
drawn away.

> The surgeon's hand sinks a fraction of a millimeter deeper into your
brain, instantly compensating its measurements and signals for the
changed position. The process is repeated for the next layer, and soon a
second simulation resides in the computer, communicating with the first
and with the remaining brain tissue. Layer after layer the brain is
simulated, then excavated. Eventually your skull is empty, and the
surgeon's hand rests deep in your brainstem. Though you have not lost
consciousness, or even your train of thought, your mind has been removed
from the brain and transferred to a machine. In a final, disorienting
step the surgeon lifts its hand. Your suddenly abandoned body dies. For
a moment you experience only quiet and dark. Then, once again, you can
open your eyes. Your perspective has shifted. The computer simulation
has been disconnected from the cable leading to the surgeon's hand and
reconnected to a shiny new body of the style, color, and material of
your choice. Your metamorphosis is complete.

