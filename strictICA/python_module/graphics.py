import csv

import numpy as np
from matplotlib import pyplot as plt
from sklearn.metrics import roc_curve, auc


def rd(filename):
    # Считывание входных данных
    res = list()
    with open(filename, 'r') as file:
        rows = csv.reader(file, delimiter='\t')
        for row in rows:
            res.append(float(row[0]))
    return res


def roc(ans, predict, title):
    fpr, tpr, _ = roc_curve(y_true=ans, y_score=predict)
    roc_auc = auc(fpr, tpr)

    plt.figure()
    lw = 2
    plt.plot(
        fpr,
        tpr,
        color="darkorange",
        lw=lw,
        label="ROC curve (area = %0.2f)" % roc_auc,
    )
    plt.plot([0, 1], [0, 1], color="navy", lw=lw, linestyle="--")
    plt.xlim([0.0, 1.0])
    plt.ylim([0.0, 1.05])
    plt.xlabel("False Positive Rate")
    plt.ylabel("True Positive Rate")
    plt.title(title)
    plt.legend(loc="lower right")
    plt.savefig('../graphics/' + title + '-img')
    plt.show()


if __name__ == '__main__':
    x = rd('../graphics/p.txt')
    y1 = rd('../graphics/p_ans_0.txt')
    y2 = rd('../graphics/p_ans_1.txt')
    y3 = rd('../graphics/p_ans_2.txt')
    y4 = rd('../graphics/p_ans_3.txt')

    x = np.array(x)
    y1 = np.array(y1, dtype=bool)
    y2 = np.array(y2, dtype=bool)
    y3 = np.array(y3, dtype=bool)
    y4 = np.array(y4, dtype=bool)

    roc(y1, x, 'p-vs-p_ans_0')
    roc(y2, x, 'p-vs-p_ans_1')
    roc(y3, x, 'p-vs-p_ans_2')
    roc(y4, x, 'p-vs-p_ans_3')
