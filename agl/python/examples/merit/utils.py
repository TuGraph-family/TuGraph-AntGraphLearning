import argparse
import numpy as np


# Utility function and class
class EarlyStopMonitor(object):
    def __init__(self, max_round=3, higher_better=True, tolerance=1e-3):
        self.max_round = max_round
        self.num_round = 0

        self.epoch_count = 0
        self.best_epoch = 0

        self.last_best = None
        self.higher_better = higher_better
        self.tolerance = tolerance

    def early_stop_check(self, curr_val):
        self.epoch_count += 1

        if not self.higher_better:
            curr_val *= -1
        if self.last_best is None:
            self.last_best = curr_val
        elif (curr_val - self.last_best) / np.abs(self.last_best) > self.tolerance:
            self.last_best = curr_val
            self.num_round = 0
            self.best_epoch = self.epoch_count
        else:
            self.num_round += 1
        return self.num_round >= self.max_round


def parse_args():
    parser = argparse.ArgumentParser(description="MERIT")

    parser.add_argument("--model", type=str, default="merit",
                        help="Choose a model:[merit, tgat]")
    parser.add_argument("--agg_type", type=str, default="conv",
                        help="Choose a type of merit aggregator :[conv, lstm]")

    return parser.parse_args()
