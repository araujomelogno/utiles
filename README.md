# Utiles

This project can be used as a starting point to create your own Vaadin application with Spring Boot.
It contains all the necessary configuration and some placeholder files to get you started.

## Running the application

Open the project in an IDE. You can download the [IntelliJ community edition](https://www.jetbrains.com/idea/download) if you do not have a suitable IDE already.
Once opened in the IDE, locate the `Application` class and run the main method using "Debug".

For more information on installing in various IDEs, see [how to import Vaadin projects to different IDEs](https://vaadin.com/docs/latest/getting-started/import).

If you install the Vaadin plugin for IntelliJ, you should instead launch the `Application` class using "Debug using HotswapAgent" to see updates in the Java code immediately reflected in the browser.

## Deploying to Production

### Audio processing

M4A/MP4 duration and splitting run entirely in Java using `org.mp4parser:muxer`,
which Maven includes in the WAR. No FFmpeg, ffprobe, executable paths, or extra
server installation are needed. The former `audio.ffprobe.path` and
`audio.ffmpeg.path` settings can be removed; they are no longer read.

The reader supports regular, extended 64-bit, and EOF-sized MP4 boxes. An EOF-sized
box is normalized in a private temporary copy, leaving the original recording
unchanged. Duration is measured from the first audio track's encoded sample timing,
so it can include a small amount of encoder padding. Other formats continue to use
jaudiotagger. Unreadable audio fails before upload rather than receiving a zero duration.

M4A/MP4 recordings longer than 20 minutes or larger than 6 MiB are remuxed into
standalone M4A containers at complete audio sample boundaries, without recoding or
splitting arbitrary bytes. Chunks are limited to 400 seconds and 6 MiB including
container headers. The first audio track is selected; video and other tracks are
excluded. Transcription timestamps use the original sample timeline, including
silence, even when the API does not return a duration. Other long audio formats
use the existing Java Sound WAV splitting when a decoder is available.

Run all tests with `./mvnw test`. Tests include real AAC fixtures, both problematic
`mdat` header variants, and a 28-minute M4A constructed in Java. They validate all
chunk containers, unchanged samples, and timestamp offsets without external
programs or OpenAI calls.

The project is a standard Maven project. To create a production build, call 

```
./mvnw clean package -Pproduction
```

If you have Maven globally installed, you can replace `./mvnw` with `mvn`.

This will build a JAR file with all the dependencies and front-end resources,ready to be run. The file can be found in the `target` folder after the build completes.
You then launch the application using 
```
java -jar target/cruds-1.0-SNAPSHOT.jar
```

## Project structure

- `MainLayout.java` in `src/main/java` contains the navigation setup (i.e., the
  side/top bar and the main menu). This setup uses
  [App Layout](https://vaadin.com/docs/components/app-layout).
- `views` package in `src/main/java` contains the server-side Java views of your application.
- `views` folder in `src/main/frontend` contains the client-side JavaScript views of your application.
- `themes` folder in `src/main/frontend` contains the custom CSS styles.

## Useful links

- Read the documentation at [vaadin.com/docs](https://vaadin.com/docs).
- Follow the tutorial at [vaadin.com/docs/latest/tutorial/overview](https://vaadin.com/docs/latest/tutorial/overview).
- Create new projects at [start.vaadin.com](https://start.vaadin.com/).
- Search UI components and their usage examples at [vaadin.com/docs/latest/components](https://vaadin.com/docs/latest/components).
- View use case applications that demonstrate Vaadin capabilities at [vaadin.com/examples-and-demos](https://vaadin.com/examples-and-demos).
- Build any UI without custom CSS by discovering Vaadin's set of [CSS utility classes](https://vaadin.com/docs/styling/lumo/utility-classes). 
- Find a collection of solutions to common use cases at [cookbook.vaadin.com](https://cookbook.vaadin.com/).
- Find add-ons at [vaadin.com/directory](https://vaadin.com/directory).
- Ask questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/vaadin) or join our [Forum](https://vaadin.com/forum).
- Report issues, create pull requests in [GitHub](https://github.com/vaadin).


## Deploying using Docker

To build the Dockerized version of the project, run

```
mvn clean package -Pproduction
docker build . -t cruds:latest
```

Once the Docker image is correctly built, you can test it locally using

```
docker run -p 8080:8080 cruds:latest
```

## Scheduled Tasks

### Odoo Project Synchronization

The application includes a scheduled task (`OdooProjectSyncTask`) that runs every hour to fetch projects from an Odoo instance and save new ones into the local database.

To enable this functionality, you need to configure the Odoo connection details in `src/main/resources/application.properties`:

```properties
# ODOO Configuration for Hourly Project Sync
# These properties are used by the OdooProjectSyncTask to connect to your Odoo instance.
# Ensure these are correctly set for the OdooProjectSyncTask to function.
# odoo.url: The full URL of your Odoo instance (e.g., http://localhost:8069).
# odoo.db: The name of your Odoo database.
# odoo.username: The username for connecting to Odoo.
# odoo.password: The API key or password for the Odoo user.
odoo.url=your_odoo_instance_url
odoo.db=your_odoo_database
odoo.username=your_odoo_username
odoo.password=your_odoo_api_key_or_password
```

This task interacts with Odoo using XML-RPC, leveraging the Apache XML-RPC client library (`org.apache.xmlrpc:xmlrpc-client`) which is included in `pom.xml`.
# utiles
# utiles
