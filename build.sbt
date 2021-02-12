import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

lazy val unique = project.in(file("."))
  .disablePlugins(MimaPlugin)
  .settings(commonSettings, releaseSettings, publish / skip := true)
  .aggregate(coreJVM, coreJS)

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("core"))
  .settings(commonSettings, releaseSettings)
  .settings(
    name := "unique"
  )
  .jsSettings(scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule)))
  .jsSettings(crossScalaVersions := crossScalaVersions.value.filterNot(_.startsWith("3.0.0-M1")))

lazy val docs = project.in(file("docs"))
  .disablePlugins(MimaPlugin)
  .enablePlugins(MicrositesPlugin)
  .enablePlugins(TutPlugin)
  .settings(commonSettings, releaseSettings, micrositeSettings, publish / skip := true)
  .dependsOn(coreJVM)
  
lazy val coreJVM = core.jvm
lazy val coreJS = core.js


val catsV = "2.3.0"
val catsEffectV = "3.0.0-M4"
val disciplineMunitV = "1.0.3"
val munitCatsEffectV = "0.11.0"

val kindProjectorV = "0.11.3"
val betterMonadicForV = "0.3.1"


lazy val contributors = Seq(
  "ChristopherDavenport" -> "Christopher Davenport"
)

val Scala212 = "2.12.12"
val Scala213 = "2.13.3"
val Scala3M1 = "3.0.0-M1"
val Scala3M2 = "3.0.0-M2"

// General Settings
lazy val commonSettings = Seq(
  organization := "io.chrisdavenport",

  scalaVersion := Scala213,
  crossScalaVersions := Seq(Scala3M2, Scala3M1, scalaVersion.value, Scala212),
  scalacOptions ++= {
    if (isDotty.value) Seq.empty
    else Seq("-Yrangepos")
  },
  scalacOptions in (Compile, doc) ++= Seq(
      "-groups",
      "-sourcepath", (baseDirectory in LocalRootProject).value.getAbsolutePath,
      "-doc-source-url", "https://github.com/christopherdavenport/unique/blob/v" + version.value + "€{FILE_PATH}.scala"
  ),
  scalacOptions in (Compile, doc) -= "-Xfatal-warnings",

  libraryDependencies ++= {
    if (isDotty.value) Seq.empty
    else Seq(
      compilerPlugin("org.typelevel" % "kind-projector" % kindProjectorV cross CrossVersion.full),
      compilerPlugin("com.olegpy" %% "better-monadic-for" % betterMonadicForV),
    )
  },
  libraryDependencies ++= Seq(
    "org.typelevel"               %%% "cats-core"                  % catsV,
    "org.typelevel"               %%% "cats-effect"                % catsEffectV,
    "org.typelevel"               %%% "discipline-munit"           % disciplineMunitV         % Test,
    "org.typelevel"               %%% "munit-cats-effect-3"        % munitCatsEffectV         % Test,
    "org.typelevel"               %%% "cats-laws"                  % catsV                    % Test,
  ),
  Compile / doc / sources := {
    val old = (Compile / doc / sources).value
    if (isDotty.value)
      Seq()
    else
      old
  }
)

lazy val releaseSettings = {
  Seq(
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/ChristopherDavenport/unique"),
        "git@github.com:ChristopherDavenport/unique.git"
      )
    ),
    homepage := Some(url("https://github.com/ChristopherDavenport/unique")),
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    pomIncludeRepository := { _ => false },
    pomExtra := {
      <developers>
        {for ((username, name) <- contributors) yield
        <developer>
          <id>{username}</id>
          <name>{name}</name>
          <url>http://github.com/{username}</url>
        </developer>
        }
      </developers>
    }
  )
}

lazy val micrositeSettings = {
  import microsites._
  Seq(
    micrositeName := "unique",
    micrositeDescription := "Functional Unique Values for Scala",
    micrositeAuthor := "Christopher Davenport",
    micrositeGithubOwner := "ChristopherDavenport",
    micrositeGithubRepo := "unique",
    micrositeBaseUrl := "/unique",
    micrositeDocumentationUrl := "https://www.javadoc.io/doc/io.chrisdavenport/unique_2.12",
    micrositeFooterText := None,
    micrositeHighlightTheme := "atom-one-light",
    micrositePalette := Map(
      "brand-primary" -> "#3e5b95",
      "brand-secondary" -> "#294066",
      "brand-tertiary" -> "#2d5799",
      "gray-dark" -> "#49494B",
      "gray" -> "#7B7B7E",
      "gray-light" -> "#E5E5E6",
      "gray-lighter" -> "#F4F3F4",
      "white-color" -> "#FFFFFF"
    ),
    fork in tut := true,
    scalacOptions in Tut --= Seq(
      "-Xfatal-warnings",
      "-Ywarn-unused-import",
      "-Ywarn-numeric-widen",
      "-Ywarn-dead-code",
      "-Ywarn-unused:imports",
      "-Xlint:-missing-interpolator,_"
    ),
    libraryDependencies += "com.47deg" %% "github4s" % "0.27.1",
    micrositePushSiteWith := GitHub4s,
    micrositeGithubToken := sys.env.get("GITHUB_TOKEN"),
    micrositeExtraMdFiles := Map(
        file("CHANGELOG.md")        -> ExtraMdFileConfig("changelog.md", "page", Map("title" -> "changelog", "section" -> "changelog", "position" -> "100")),
        file("CODE_OF_CONDUCT.md")  -> ExtraMdFileConfig("code-of-conduct.md",   "page", Map("title" -> "code of conduct",   "section" -> "code of conduct",   "position" -> "101")),
        file("LICENSE")             -> ExtraMdFileConfig("license.md",   "page", Map("title" -> "license",   "section" -> "license",   "position" -> "102"))
    )
  )
}

ThisBuild / crossScalaVersions := Seq(Scala213)
ThisBuild / scalaVersion := Scala213
ThisBuild / githubWorkflowPublishTargetBranches := Seq()
ThisBuild / githubWorkflowJavaVersions := Seq("adopt@1.11")
ThisBuild / githubWorkflowArtifactUpload := false
ThisBuild / githubWorkflowBuildMatrixAdditions +=
  "ci" -> List("test")
ThisBuild / githubWorkflowBuild :=
  Seq(WorkflowStep.Sbt(List("${{ matrix.ci }}"), name = Some("Validation")))
//ThisBuild / githubWorkflowAddedJobs ++= Seq(
//  WorkflowJob(
//    "microsite",
//    "Microsite",
//    githubWorkflowJobSetup.value.toList ::: List(
//      WorkflowStep.Use(
//        UseRef.Public("ruby", "setup-ruby", "v1"),
//        name = Some("Setup Ruby"),
//        params = Map("ruby-version" -> "2.6", "bundler-cache" -> "true")
//      ),
//      WorkflowStep.Run(List("gem install sass"), name = Some("Install SASS")),
//      WorkflowStep.Run(List("gem install jekyll -v 2.5"), name = Some("Install Jekyll")),
//      WorkflowStep.Sbt(List("docs/publishMicrosite"), name = Some("Build microsite"))
//    ),
//    scalas = List(Scala213)
//  )
//)
