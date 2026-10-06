import unittest

from plot_speedup import aggregate_measurements


def measurement(threads, run, response, processing="5", count="7", status="SUCCESS"):
    return {
        "size": "20", "mode": "platform", "threads": str(threads),
        "run": str(run), "responseTimeMs": str(response),
        "processingTimeMs": processing, "duplicatesFound": count, "status": status,
    }


class AggregateMeasurementsTest(unittest.TestCase):
    def test_speedup_uses_http_time_and_each_configuration_average(self):
        rows = [measurement(1, 1, 50), measurement(1, 2, 150),
                measurement(2, 1, 20), measurement(2, 2, 30)]
        actual = aggregate_measurements(rows)
        self.assertEqual(actual[0]["avgResponseTimeMs"], "100.00")
        self.assertEqual(actual[1]["avgResponseTimeMs"], "25.00")
        self.assertEqual(actual[1]["speedup"], "4.000000")

    def test_empty_collection_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "não contém medições"):
            aggregate_measurements([])

    def test_different_functional_results_are_rejected(self):
        with self.assertRaisesRegex(ValueError, "contagens.*divergentes"):
            aggregate_measurements([measurement(1, 1, 50), measurement(2, 1, 20, count="8")])

    def test_unsuccessful_or_nonfinite_measurements_are_rejected(self):
        for response, status in [(50, "ERROR"), ("NaN", "SUCCESS"), (0, "SUCCESS")]:
            with self.subTest(response=response, status=status):
                with self.assertRaisesRegex(ValueError, "inválida"):
                    aggregate_measurements([measurement(1, 1, response, status=status)])

    def test_duplicate_or_missing_runs_are_rejected(self):
        for rows in ([measurement(1, 1, 50), measurement(1, 1, 50)],
                     [measurement(1, 2, 50)]):
            with self.subTest(rows=rows):
                with self.assertRaises(ValueError):
                    aggregate_measurements(rows)

    def test_sequential_baseline_is_required(self):
        with self.assertRaisesRegex(ValueError, "falta.*sequencial"):
            aggregate_measurements([measurement(2, 1, 50)])

    def test_different_numbers_of_repetitions_are_rejected(self):
        with self.assertRaisesRegex(ValueError, "número de repetições"):
            aggregate_measurements([measurement(1, 1, 50), measurement(1, 2, 50),
                                    measurement(2, 1, 20)])


if __name__ == "__main__":
    unittest.main()
