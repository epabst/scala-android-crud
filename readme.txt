A Scala framework for supporting CRUD on the Android Platform between UI, Persistence, and Model

See the Wiki for more information: 
   https://github.com/epabst/scala-android-crud/wiki

Here are some topics that will be added soon to the Wiki:

* Getting Started:
   * CrudType and CrudApplication (refer to Scaladocs)
   * Naming Conventions for layout files, strings, DB tables, entityName, etc.
   * Supported Field Types: int, long, double, currency, Date, Calendar, String, Enumeration,
   * Supported Fields: both forms of viewId
   * How to indicate if the Entity is Updateable and/or Displayable
   * Parent Fields and Foreign Keys
   * Generating Layout (refer to Scaladocs)
   * UriPath (refer to Scaladocs)
* Ready-to-use Enhancements:
   * Integrating with an Object Model: fields and findAll
   * DerivedCrudPersistence and overriding idField
   * Generated Fields
   * Backup Service
* Under the Hood
   * Built-In Navigation, including Actions (refer to Scaladocs)
   * What subjects are copied to what subjects and when
   * Logging - SLF4J
* How to Customize
   * Fields
   * Activity
   * Interacting with other Activities
   * Customizing Navigation
* Future Features
   * Buttons and Links - For now these must be handled by customizing the app.
