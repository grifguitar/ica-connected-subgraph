import math
import numpy as np
from scipy.linalg import inv, sqrtm
import matplotlib.pyplot as plt
import csv
import random


def solve():
    # Считывание входных данных
    x = list()
    y = list()
    with open('simple_mtx.csv', 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter='\t')
        for row in plots:
            if random.randint(0, 0) == 0:
                x.append(float(row[0]))
                y.append(float(row[1]))
    len_x = len(x)
    len_y = len(y)
    assert len_x == len_y

    x, y = calc_mean(x, y)

    # Построение ковариационной матрицы
    cov_matrix = get_cov_matrix(x, y)

    # Вычисление отбеливающей матрицы двумя способами
    w1 = inv(sqrtm(cov_matrix))
    w2 = sqrtm(inv(cov_matrix))

    # Проверка, что w1 == w2
    print('-----')
    print(w1)
    print(w2)

    # Проверка, что выполняются свойства отбеливающей матрицы
    print('-----')
    print(np.matmul(w1.transpose(), w1))
    print(inv(cov_matrix))

    # Преобразование компонент x и y в матрицу arg
    arg = list()
    for i in range(len_x):
        arg.append([x[i], y[i]])
    arg = np.array(arg)

    # Отбеливание матрицы arg
    result_var = np.matmul(w1, arg.transpose())

    # Преобразование матрицы result_var на компоненты result_x и result_y
    result_x = list()
    result_y = list()
    for i in range(len_x):
        result_x.append(result_var[0][i])
        result_y.append(result_var[1][i])

    # Запись в файл
    f = open('output.txt', 'w')
    for i in range(len_x):
        f.write(str(result_x[i]))
        f.write(' ')
        f.write(str(result_y[i]))
        f.write('\n')
    f.close()

    # Проверка, что у полученного выбеленного вектора result_var
    # действительно единичная ковариационная матрица
    get_cov_matrix(result_x, result_y)

    # Построение графиков
    plt.xlabel('xs')
    plt.ylabel('ys')
    plt.scatter(x, y, s=1)
    plt.xlim(-2.5, 2.5)
    plt.ylim(-2.5, 2.5)
    plt.gca().set_aspect('equal', adjustable='box')
    plt.draw()
    plt.show()
    plt.xlabel('xs')
    plt.ylabel('ys')
    plt.scatter(result_x, result_y, s=1)
    plt.xlim(-2.2, 2.2)
    plt.ylim(-2.2, 2.2)
    plt.gca().set_aspect('equal', adjustable='box')
    plt.draw()
    plt.show()

    total_q = l1pca(result_var.transpose())
    plt.xlabel('xs')
    plt.ylabel('ys')
    plt.scatter(result_x, result_y, s=1)
    plt.plot([0, total_q[0]], [0, total_q[1]], color='orange')
    plt.xlim(-2.2, 2.2)
    plt.ylim(-2.2, 2.2)
    plt.gca().set_aspect('equal', adjustable='box')
    plt.draw()
    plt.show()


def calc_mean(x, y):
    assert len(x) == len(y)
    n = len(x)
    e_x = sum(x) / n
    e_y = sum(y) / n
    new_x = list()
    new_y = list()
    for i in range(n):
        new_x.append(x[i] - e_x)
        new_y.append(y[i] - e_y)
    return new_x, new_y


def get_cov_matrix(x, y):
    assert len(x) == len(y)
    n = len(x)
    e_x = sum(x) / n
    e_y = sum(y) / n
    assert abs(e_x) < 1e-5
    assert abs(e_y) < 1e-5
    ans1 = list()
    ans2 = list()
    ans3 = list()
    ans4 = list()

    for i in range(n):
        ans1.append((x[i] - e_x) * (x[i] - e_x))
        ans2.append((x[i] - e_x) * (y[i] - e_y))
        ans3.append((y[i] - e_y) * (x[i] - e_x))
        ans4.append((y[i] - e_y) * (y[i] - e_y))

    assert len(ans1) == len(ans2) == len(ans3) == len(ans4)

    cov_matrix = [[sum(ans1), sum(ans2)],
                  [sum(ans3), sum(ans4)]]
    cov_matrix = np.array(cov_matrix) / n

    print('E:')
    print(e_x, e_y)
    print('cov_matrix:')
    print(cov_matrix)

    return cov_matrix


def gen_circle():
    s2 = math.sqrt(2)
    step = 200
    dt = s2 / step
    y2 = s2 / 2
    x1 = -y2

    points = list()
    for i in range(step):
        x1 = x1 + dt
        y1 = math.sqrt(1 - x1 * x1)
        y2 = y2 - dt
        x2 = math.sqrt(1 - y2 * y2)
        points.append([x1, y1])
        points.append([x2, y2])
        points.append([-x1, -y1])
        points.append([-x2, -y2])

    points = np.array(points)
    point_x = list()
    point_y = list()
    for i in range(len(points)):
        point_x.append(points[i][0])
        point_y.append(points[i][1])

    plt.xlabel('xs')
    plt.ylabel('ys')
    plt.scatter(point_x, point_y, s=1)
    plt.xlim(-1.05, 1.05)
    plt.ylim(-1.05, 1.05)
    plt.gca().set_aspect('equal', adjustable='box')
    plt.draw()
    plt.show()
    return points


def l1pca(args):
    # Генерируем 800 точек на единичной окружности
    points = gen_circle()
    # Инициализируем total_min и total_q
    total_min = 999999999
    total_q = np.array([0, 0])
    # Переберем все точки
    for i in range(len(points)):
        # найдем вектор q
        q = points[i]
        # выполним матричное умножение на вектор q
        res = np.matmul(args, q)
        # найдем L1-норму полученного вектора
        curr_val = np.sum(np.absolute(res))
        # обновим минимум
        if curr_val < total_min:
            total_min = curr_val
            total_q = points[i]
    print('total_min: ', total_min)
    print('values: ', total_q)

    new_q = 1.5 * total_q / len(args)
    new_res = np.matmul(args, new_q)
    new_val = np.sum(np.absolute(new_res))

    print('new value L1 norm: ', new_val)
    print('new values: ', new_q)

    return total_q


if __name__ == '__main__':
    solve()
