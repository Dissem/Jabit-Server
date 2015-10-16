Jabit Server
============

This is the server node using the Jabit library. You can run it by calling
```
java -jar jabit-server.jar
```
The interface will be available on port 9000, Bitmessage as usual on Port 8444.

There are still a few problems with the interface (the idea is to allow collecting
and displaying broadcasts).

On first startup it will create a config file (allowing you to configure the
Bitmessage port), a whitelist, a blacklist and a shortlist. If the whitelist isn't
empty, the blacklist will be irrelevant. You can disable the feature by simply
adding a valid Bitmessage address to the whitelist. For shortlisted addresses, only
the last five broadcasts are displayed and stored (useful e.g. for time services or
Q's Aktivlist).

Building / Development
----------------------

You can build the jar file with
```
./gradlew build
```
As there is a problem with the build order, you'll need to do this twice.

To deploy on a Ubuntu server (might work on other Linuxes as well), create a file
`/etc/init/jabit.conf` with the following contents:
```
chdir /srv/jabit

exec su -s /bin/sh -c 'exec "$0" "$@"' jabit -- /usr/bin/java -jar jabit-server.jar --server.port=9000 > /dev/null

start on runlevel [2345]
stop on runlevel [^2345]

```
there must be a user jabit and a folder `/srv/jabit` where this user has write
permission containing `jabit-server.jar`.