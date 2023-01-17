package = 'elle-cli'
version = 'scm-1'

description = {
	summary = [[ A command-line frontend to transactional consistency checkers
	             for black-box databases. ]],
    detailed = [[
	is a command-line frontend to transactional consistency checkers for
	black-box databases. In comparison to Jepsen library it is standalone and
	language-agnostic tool. You can use it with tests written in any
	programming language and everywhere where JVM is available. Under the hood
	elle-cli uses libraries Elle, Knossos and Jepsen and provides the same
	correctness guarantees.
    ]],
    homepage = "https://github.com/ligurio/elle-cli",
    maintainer = "Sergey Bronnikov <estetus@gmail.com>",
    license = "Eclipse Public License version 1.0",
}

source  = {
    url = 'git+https://github.com/ligurio/elle-cli.git',
    branch = 'master',
}

build = {
    type = 'make',
    copy_directories = {},
    variables = {
        CMAKE_INSTALL_PREFIX = "$(PREFIX)",
    },
}
