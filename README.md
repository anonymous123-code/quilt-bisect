# Quilt Bisect

A mod helping in blaming mods for issues.

## Usage

This project uses Loader Plugins, `-Dloader.experimental.allow_loading_plugins=true` needs to be added to your JVM arguments.

## Development

While it works in a development environment, debugging currently fails. ([#3](https://github.com/anonymous123-code/quilt-crasher/issues/3))
For testing, I also recommend [Quilt Crasher](https://github.com/anonymous123-code/quilt-crasher), which I developed to enable testing of this kind of mod.

## Licensing

`PluginLogger` and `GracefulTerminator` were mostly taken from https://github.com/comp500/ModVote.
`BisectPlugin` also contains large parts of `ModvotePlugin` from that repo.
All of these are under the [MIT license](Modvote-License) (by comp500)
