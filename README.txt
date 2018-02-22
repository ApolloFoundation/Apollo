Running the Apl software:

Dependencies: Java 8 or later needs to be installed first. Oracle JVM gives
better performance and has been more tested, but OpenJDK is also supported.


Using the installer:

An IzPack based installation package is provided. Click on the corresponding
jar/exe/dmg package, or run "java -jar apl-client.jar" to start the
installer. See https://bitbucket.org/JeanLucPicard/apl/issues/283 for more
details about the IzPack installer. After installation, use the shortcuts or
desktop icons to start the Apl server.


Using the apl-client.zip package:

Unpack the apl-client.zip package and open a shell in the resulting apl
directory. Execute the run.sh or start.sh script if using Linux or BSD, run.bat
if using Windows, or run.command if using Mac.

On Unix, the run.sh script must be run from within the installation directory,
and uses this directory to search for configuration files, store logs, and for
the apl_db blockchain database. The start.sh script can be run from any
directory, it starts the java process in the background and uses ~/.apl for
configuration files, logs, and the apl_db database. Unlike run.sh, start.sh
uses desktop mode, creating a desktop tray icon and opening the JavaFX UI if
supported.

The initialization takes a few seconds. When it is ready, you should see the
message "Apl server 1.x.x started successfully" in the console log. If run in
desktop mode, a JavaFX window will open automatically. Otherwise, open a
browser, without stopping the java process, and go to http://localhost:7876 ,
where the Apl UI should now be available.

To stop the application, type Ctrl-C inside the console window, or use the
stop.sh script if started with start.sh.

Warning: It is better to use only latin characters and no spaces in the path
to the Apl installation directory, as the use of special characters may result
in permissions denied error in the browser, which is a known jetty issue.


Customization:

There are many configuration parameters that could be changed, but the defaults
are set so that normally you can run the program immediately after unpacking,
without any additional configuration. To see what options are there, open the
conf/apl-default.properties file. All possible settings are listed, with
detailed explanation. If you decide to change any setting, do not edit
apl-default.properties directly, but create a new conf/apl.properties file
and only add to it the properties that need to be different from the default
values. You do not need to delete the defaults from apl-default.properties, the
settings in apl.properties override those in apl-default.properties. This way,
when upgrading the software, you can safely overwrite apl-default.properties
with the updated file from the new package, while your customizations remain
safe in the apl.properties file.


How to contribute?

There are many ways to contribute to Apl. Here are some examples:

 * create pull requests
 * review pull requests
 * review existing code
 * create issues (aka feature ideas, bug reports, documentation etc.)
 * answer issues


Technical details:

The Apl software is a client-server application. It consists of a java server
process, the one started by the run.sh script, and a javascript user interface
run in a browser. A JavaFX UI is also available and starts automatically on
supported configurations. To run a node, forge, update the blockchain, interact
with peers, only the java process needs to be running, so you could logout and
close the browser but keep the java process running. If you want to keep
forging, make sure you do not click on "stop forging" when logging out. You can
also just close the browser without logging out.

The java process communicates with peers on port 7874 tcp by default. If you are
behind a router or a firewall and want to have your node accept incoming peer
connections, you should setup port forwarding. The server will still work though
even if only outgoing connections are allowed, so opening this port is optional.

The user interface is available on port 7876. This port also accepts http API
requests which other Apl client applications could use.

The blockchain is stored on disk using the H2 embedded database, inside the
apl_db directory. When upgrading, you should not delete the old apl_db
directory, upgrades always include code that can upgrade old database files to
the new version whenever needed. But there is no harm if you do delete the
apl_db, except that it will take some extra time to download the blockchain
from scratch.

The default Apl client does not store any wallet-type file on disk. Unlike
bitcoin, your password is the only thing you need to get access to your account,
and is the only piece of data you need to backup or remember. This also means
that anybody can get access to your account with only your password - so make
sure it is long and random. A weak password will result in your funds being
stolen immediately.

The java process logs its activities and error messages to the standard output
which you see in the console window, but also to a file apl.log, which gets
overwritten at restart. In case of an error, the apl.log file may contain
helpful information, so include its contents when submitting a bug report.

In addition to the default user interface at http://localhost:7876 , the
following urls are available:

http://localhost:7876/test - a list of all available http API requests, very
useful for client developers and for anyone who wants to execute commands
directly using the http interface without going through the browser UI.

http://localhost:7876/test?requestType=<specificRequestType> - same as above,
but only shows the form for the request type specified.

http://localhost:7876/doc - a javadoc documentation for client developers who
want to use the Java API directly instead of going through the http interface.


Compiling:

The source is included in the src subdirectory. To compile it on unix, just run
the enclosed compile.sh script. This will compile all java classes and put them
under the classes subdirectory, which is already in the classpath used by the
run.sh startup script. The compiled class files can optionally be packaged in a
apl.jar file using the enclosed jar.sh script, and then apl.jar should be
included in the classpath instead of the classes subdirectory.

