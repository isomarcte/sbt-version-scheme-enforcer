# SBT Version Scheme Enforcer #

The SBT Version Scheme Enforcer plugin is a plugin which automatically configures [Migration Manager (MiMa)][mima] to verify the binary compatibility constraints of your library.

# TL;DR How Do I Turn It On #


If you are using git, then all you need to do is add the plugin to your `project/plugins.sbt` file. If you are _not_ using git, then you will additionally have to set `versionSchemeEnforcerPreviousVersion` with your previous version.

```scala
addSbtPlugin("io.isomarcte" % "sbt-version-scheme-enforcer-plugin" % "2.0.0.1")
```

And ensure you've set `versionScheme` in your `build.sbt`.

```scala
ThisBuild / versionScheme := Some("early-semver") // or Some("pvp") or Some("semver-spec")
```

That's it! Now all your MiMa settings will be automatically derived.

Because your intended binary compatibility guarantees is directly a function of the versionScheme, previous version, and next
version, everything else can be derived.

The previous version of your project will be detected from the current commits most recent git tag.

You can then run either `mimaReportBinaryIssues` as normal or `versionSchemeEnforcerCheck` (differences explained below).

If you are using a multi-module build and you don't want to run this on your the root project, then add this to your root project only.

```scala
lazy val root = (project in file(".")).settings(/* settings */).disablePlugins(SbtVersionSchemeEnforcerPlugin)
```


# Overview #

The SBT Version Scheme enforcer plugin aims to allow library authors to configure [mima][mima] for their library with minimal effort and is unopinionated about the version scheme you choose to use.

# Requirements #

* SBT >= 1.4.0

# Optional #

* git on the PATH (for automatic previous version calculation)

# Supported Version Schemes #

The currently supported versioning schemes are,

* [Package Versioning Policy (PVP)][pvp]
* [Semantic Versioning][semver]
* [Early Semantic Versioning][early-semver]

# SBT Tasks/Settings #

This is the full set of SBT Tasks and Settings this plugin provides. _Usually_ you won't need to bother with most of them.

You can view them in code here: https://github.com/isomarcte/sbt-version-scheme-enforcer/blob/main/plugin/src/main/scala/io/isomarcte/sbt/version/scheme/enforcer/plugin/Keys.scala

_Any_ setting can be manually set at the project level and it will be left alone by the plugin.

## Settings ##

Name | Type | Description
---- | ---- | -----------
versionSchemeEnforcerPreviousVersion | `Option[String]` | Previous version to compare against the current version for calculating binary compatibility. If you are using `git` and you have a tag as an ancestor to the current commit, this will be automatically derived.
versionSchemeEnforcerChangeType | `Either[Throwable, VersionChangeType]` | The type of binary change. It is used to configured MiMa settings. Normally this is derived from versionSchemeEnforcerPreviousVersion and should not normally be set directly. If it results in an error and versionSchemeEnforcerCheck is run, that error is raised.
versionSchemeEnforcerIntialVersion | `Option[String]` | The initial version which should have the versionScheme enforced. If this is set then verions <= to this version will have Mima configured to not validate any binary compatibility constraints. This is particularly useful when you are adding a new module to an exsiting project.
versionSchemeEnforcerPreviousTagFilter | `String => Boolean` | A filter used when determining the previous version from a VCS tag. The selected tag will be the most recent tag, reachable from the current commit, for which this filter returns true. A common use case for this is not considering pre-release in binary compatibility checks. For example, assuming your versionScheme is Semver or Early Semver, if you are releasing 1.1.0-M3, you may want to consider binary compatibility compared to the last 1.0.x release, and permit arbitrary binary changes between various milestone releases. By default, comparing two versions which have the same numeric base version will imply that no visible changes have been made to the binary API, e.g. comparing 1.1.0-M2 to 1.1.0-M3 will yield a binary change type of Patch (assuming Semver or Early Semver.)

## Tasks ##

Name | Type | Description
---- | ---- | -----------
versionSchemeEnforcerCheck | `Unit` | Verifies that the sbt-version-scheme-enforcer settings are valid and runs MiMa with the derived settings.

### Differnces Between `versionSchemeEnforcerCheck` And `mimaReportBinaryIssues` ###

`versionSchemeEnforcerCheck` and `mimaReportBinaryIssues` are very similar, and in fact _most_ of what `versionSchemeEnforcerCheck` does is just run `mimaReportBinaryIssues`. The only significant difference is that `versionSchemeEnforcerCheck` will raise an error if any of the settings required from validating the version scheme are missing or invalid. For example, if you don't set `versionScheme` or set it to an invalid value `versionSchemeEnforcerCheck` will fail. On the other hand, **if any of the settings required for verifying the version scheme are invalid/missing _none_ of the mima settings are modified by this plugin.**

This means that you can still run `mimaReportBinaryIssues` and this plugin will stay out of your way.

# FAQ #

## How Do I Ignore Certain Tags From Being Considered For The Previous Version (Milestones) ##

As of version 2.0.0.0 of this plugin when determining the previous version from a VCS tag, e.g. a git tag, you can provide a filter to remove certain tags from consideration. This is done by setting the `versionSchemeEnforcerPreviousTagFilter` key. The tag selected will be the most recent tag, reachable from the current commit, for which the function defined by `versionSchemeEnforcerPreviousTagFilter` returns `true`.

A common use case for this is filtering out milestone releases. Consider if we have a project (using PVP) and we are attempting to release version `1.1.0.0-M2`. By default, the previous version selected from the VCS tag will be `1.1.0.0-M1`, which will yield a numeric version of `1.1.0.0`. This would result in a version change type of `Patch`, which would mean that the publicly exported symbols (methods/functions/class/etc.) must have not changed at all. This is _probably_ not what we want. We likely want to compare `1.1.0.0-M2` with the most recent full release, in our example let's say that is `1.0.2.1`. Comparing `1.0.2.1` and `1.1.0.0-M1` under PVP would mean that this is a binary breaking release and that any symbols may have been removed or added between the two version, e.g. it is a `Major` change type. We can achieve this behavior by setting `versionSchemeEnforcerPreviousTagFilter` like so,

```scala

ThisBuild / versionSchemeEnforcerPreviousTagFilter := {(value: String) =>
  if(value.matches(""".*-M\d+$""")) {
      false
    } else {
      true
    }
  }

```

This will automatically filter out any tags which end in a `-M\d+`, e.g. `-M2`. As this is a common use case a prebuilt filter is available in `TagFilters`.

```scala
// You need to fully qualify the import to avoid conflicts with sbt.io
import _root_.io.isomarcte.sbt.version.scheme.enforcer.plugin.TagFilters

ThisBuild / versionSchemeEnforcerPreviousTagFilter := TagFilters.noMilestoneFilter

```

## Why Does This Plugin Exist ##

There are a couple other plugins out there which provide similar features compared to this plugin. In particular [SBT Version Policy][sbt-version-policy] and [SBT Mima Version Check][sbt-mima-version-check]. These plugins are both great. [sbt-version-policy][sbt-version-policy] also provides some additional features which this plugin does not, namely checking the versioning information of dependencies.

However, the primary reason that I decided to write this plugin is that both [sbt-version-policy][sbt-version-policy] and [sbt-mima-version-check][sbt-mima-version-check] _only_ support configuring Mima for [Early SemVer][early-semver], and in the case of [sbt-version-policy][sbt-version-policy] they indicated that at this time they didn't intend to support any other versioning scheme ([pvp][pvp] or [semver][semver]). There are some use cases in versioning which [Early SemVer][early-semver] is fundamentally unable to express, namely supporting multiple long lived versions of the same project.

### Example ###

For example, let's say that you have a library which depends on [Cats Effect][cats-effect] at version 2.x.x. Now you want to update your library to use the new upcoming [cats-effect][cats-effect] 3 release, but you still want to maintain an old version for your users who haven't yet updated for [Cats Effect 3][cats-effect]. In a [SemVer][semver] or [Early SemVer][early-semver] world you update your library from version `1.2.3` to `2.0.0`. At this point you can keep release `1.x.x` branches _as long as you never break binary compatibility_. If this fits your use case, then you are all set, however if you _do_ end up needing to make a binary incompatible release on the `1.x.x` branch (perhaps becomes some other library you depend on forced a binary incompatible update), then you are in an difficult situation. What version should your new `1.x.x` release have? By definition, it can't be a `1.x.x` release, nor can it be `2.x.x` because that is already in use for your [Cats Effect 3][cats-effect] branch, and obviously `3.0.0` would be _valid_ but _extremely confusing_ for your users.

However, if you are using [pvp][pvp] then the situation is different. In [PVP][pvp] the first _two_ version numbers both describe a binary breaking change.

For example,

* `0.0.0.1` -> `0.1.0.0` is a binary incompatible change.
* `0.1.0.0` -> `0.2.0.0` is a binary incompatible change.
* `0.2.0.0` -> `1.0.0.0` is a binary incompatible change.
* `0.0.0.1` -> `1.0.0.0` is a binary incompatible change.
* `0.1.0.1` -> `1.0.0.0` is a binary incompatible change.

Taking our [Cats Effect 3][cats-effect] example above. If we were maintaining two versions of our library, one [Cats Effect 2][cats-effect] and one [Cats Effect 3][cats-effect], then we would have versions `0.1.0.0` and `1.0.0.0` respectively. If we needed for some reason to make a binary breaking change to our [Cats Effect 2][cats-effect] branch, then we can release at version `0.2.0.0`. This version indicates that we have broken binary compatibility with `0.1.0.0` but importantly _also_ indicates that `0.2.0.0` -> `1.0.0.0` is _also_ binary breaking.

### Summary ###

To be very clear, maintaining multiple long lived branches like that is difficult and requires significantly more work from the library authors. If you don't have that use case and are sure you never will, then [semver][semver] or [Early SemVer][early-semver] are both perfectly good versioning schemes, with the obvious advantage that they are more widely understood (especially in the JVM community). However, if you do have that use case (and I personally do), then [PVP][pvp] allows you to handle it. As a final motivating example, a project which _does_ have this use case is actually the Scala compiler/standard library itself. Scala `2.11.x` is binary incompatible with `2.12.x` is binary incompatible with `2.13.x` is binary incompatible `3.x.x`. While perhaps not intentionally, this is a good example of [pvp][pvp] versioning and its utility.

[mima]: https://github.com/lightbend/mima "Migration Manager"

[pvp]: https://pvp.haskell.org "Package Versioning Policy"

[semver]: https://semver.org/ "SemVer"

[early-semver]: https://scala-lang.org/blog/2021/02/16/preventing-version-conflicts-with-versionscheme.html "Early SemVer"

[sbt-version-policy]: https://github.com/scalacenter/sbt-version-policy "SBT Version Policy"

[sbt-mima-version-check]: https://github.com/ChristopherDavenport/sbt-mima-version-check "SBT Mima Version Check"

[cats-effect]: https://github.com/typelevel/cats-effect "Cats Effect"
