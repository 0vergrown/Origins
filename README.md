# Origins (Overgrown)

Origins addon for the [Apoli](https://github.com/Overgrown/Apoli) library mod. Adds the
Origins system — origins, layers, the choose-on-first-join GUI, and the Orb of Origin —
without bundling any power/condition/action types of its own. Everything in the
`origins:` namespace falls back to `apoli:` so existing Origins data packs work as-is.

## Status

Scaffold only. The Apoli foundation (`NamespaceAlias` API + `entity_action_chosen`
hook on `apoli:action_on_callback`) is in place; the data model, screens, networking,
and Orb of Origin item are TODO.

## Building locally

Origins depends on Apoli via the local maven cache. Publish Apoli once (re-publish after
any change you want Origins to see):

```
(cd ../../Apoli/Apoli-Fabric-1.20.1 && ./gradlew publishToMavenLocal)
./gradlew build
```
