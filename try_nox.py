import nox

# All sessions run in the system Python environment, no virtualenvs
@nox.session(venv_backend="none")
def install_deps(session):
    """
    Ensure required dependencies are installed.
    """
    session.run("pip", "install", "-r", "requirements.txt", external=True)

@nox.session(venv_backend="none")
def check_deps(session):
    """
    Verify that all dependencies are present and compatible.
    """
    session.run("pip", "check", external=True)

@nox.session(venv_backend="none")
def unit_tests(session):
    """
    Run unit tests and collect coverage data.
    """
    session.run("pytest", "--cov=your_package", "--cov-report=term-missing", "tests/unit", external=True)

@nox.session(venv_backend="none")
def db_check(session):
    """
    Test database connectivity and access.
    """
    session.run("pytest", "tests/test_db_connection.py", external=True)

@nox.session(venv_backend="none")
def regression_tests(session):
    """
    Run regression tests using pytest-regressions.
    """
    session.run("pytest", "--regressions", "tests/regressions", external=True)

@nox.session(venv_backend="none")
def full_check(session):
    """
    Orchestrator: runs all checks and tests.
    """
    session.notify("install_deps")
    session.notify("check_deps")
    session.notify("unit_tests")
    session.notify("db_check")
    session.notify("regression_tests")
