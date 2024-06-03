# ERDDAP™ Priority Projects

This is subject to change. Priorities will shift and new projects will be added over time. This is roughly in priority order, but not a strict ordering. Also some projects will take a very long time and so will likely be done slowly over time.* Changes to encourage more community contributions/improve the developer experience

  - Migrate tests to JUnit and ensure they can be run without manual set up on a new developer’s machine.

  - Set up automation to run tests for every pull request to verify changes.

  - Update documentation to be in line with modern open source project expectations.

  - Define a process for issues, feature requests, and pull requests to make sure the community knows what to expect and how to interact with the project.

  - Populate issues and feature requests with tasks from the extensive backlog.

* Detect problems

  - Add a logging system like Sentry (opt-in for admins) that can report errors and usage information to detect problems early and help our understanding of ERDDAP™ usage.

* Improve the admin experience

  - Make ERDDAP™ easier to run and configure, including but not limited to official support for containers/Kubernetes.

  - Identify pain points in admin workflows (adding datasets) and improve them.

    - One possibility is to update the XML parser to allow for modern XML, including XInclude which could reduce repetitive entries being needed.[ https://github.com/ERDDAP/erddap/issues/112](https://github.com/ERDDAP/erddap/issues/112) We should evaluate the whole problem of maintaining datasets.xml and see if there’s a better total solution.

* Support the interest of those supplying data

  - Add functionality to show how to cite a dataset

* Improve the User experience

  - Define an ERDDAP™ API for protected datasets that can easily be used directly from code.[ https://github.com/ERDDAP/erddap/issues/92](https://github.com/ERDDAP/erddap/issues/92)

* Technical Debt

  - Use a templating language or framework for defining HTML.
