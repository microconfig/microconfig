"""Regression checks for expression validation scenarios, without requiring Native Image."""

import importlib.util
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

PROJECT = Path(__file__).resolve().parents[4]
SCRIPT = PROJECT / ".github/scripts/native/verify-native.py"
spec = importlib.util.spec_from_file_location("verify_native", SCRIPT)
verify_native = importlib.util.module_from_spec(spec)
spec.loader.exec_module(verify_native)


class VerifyNativeTest(unittest.TestCase):
    def test_missing_component_is_not_an_expression_rejection(self):
        self.assertFalse(verify_native.rejected_expression_failed({
            "exit_code": 255, "files": {}, "output": "Component 'rejected00' doesn't exist.\n",
        }))

    def test_success_or_written_output_is_not_an_expression_rejection(self):
        for status, files in ((0, {}), (255, {"app/application.yaml": "digest"})):
            with self.subTest(status=status, files=files):
                self.assertFalse(verify_native.rejected_expression_failed({
                    "exit_code": status, "files": files, "output": "Can't evaluate #{null}",
                }))

    @unittest.skipUnless(os.environ.get("MICROCONFIG_CLI_JAR"), "Set MICROCONFIG_CLI_JAR to run CLI scenarios")
    def test_every_negative_expression_reaches_the_evaluator(self):
        with tempfile.TemporaryDirectory(prefix="native-expression-regression-") as directory:
            work = Path(directory)
            cases, _ = verify_native.expression_cases(PROJECT, work)
            rejected = [(name, arguments) for name, arguments in cases if "/rejected" in name]
            self.assertEqual(22, len(rejected))
            for index, (name, arguments) in enumerate(rejected):
                with self.subTest(name=name):
                    destination = work / f"output-{index}"
                    process = subprocess.run([
                        os.environ.get("MICROCONFIG_JAVA", "java"), "-jar", os.environ["MICROCONFIG_CLI_JAR"],
                        *arguments, "-d", str(destination), "-color", "false",
                    ], capture_output=True, text=True, timeout=15)
                    result = {"exit_code": process.returncode, "files": verify_native.digest_tree(destination),
                              "output": process.stdout + process.stderr}
                    self.assertTrue(verify_native.rejected_expression_failed(result), result)


if __name__ == "__main__":
    unittest.main()
