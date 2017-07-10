# Regression test howto
These files are used for testing regressions in the article import routine.
To run the tests (`ArticleConverterRegressionTest.scala`) the environment varibles
`MIGRATION_HOST`, `MIGRATION_USER` and `MIGRATION_PASSWORD`  must be set (see deploy repo for what values to set them to).

The regression test will use all .json-files in this directory.

## How to add regression-test-data
1. Import an article (to your local dev environment) which is tested and looks good.
2. Copy the saved article directly from the database
3. Create a new file in this directory. The file extension must be `.json`.
4. Paste the imported article into this file.
5. Add a new field `nodeId` and set it to the **node id** from the old ndla.no

