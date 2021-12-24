import numpy as np
import matplotlib.pyplot as plt
from scipy import signal
import csv

from sklearn.decomposition import FastICA, PCA
from sklearn.cluster import DBSCAN

episode_num = 2
img_cnt = 0


def standardize(X):
    # Standardize signals matrix
    st_dev = X.std(axis=0)
    print("standard deviation:", st_dev)
    X /= st_dev
    return X


def array_print(M, min_value=None, max_value=None, name=None):
    global img_cnt
    img_cnt += 1
    shape = np.array(M.shape)
    print(shape)
    plt.imshow(M, interpolation="nearest")
    if (min_value is not None) and (max_value is not None):
        plt.clim(vmin=min_value, vmax=max_value)
    plt.colorbar()
    plt.xlim(-1, shape[1])
    plt.ylim(-1, shape[0])
    plt.gca().set_aspect('auto', adjustable='box')
    plt.title(name)
    plt.savefig('images_{x}/frame_{y}.png'.format_map({'x': episode_num, 'y': img_cnt}))
    plt.show()


def signals_gen():
    # Generate sample data
    np.random.seed(0)
    n_samples = 2000
    time = np.linspace(0, 8, n_samples)

    # Signals
    s1 = np.sin(2 * time)  # Signal 1 : sinusoidal signal
    s2 = np.sign(np.sin(3 * time))  # Signal 2 : square signal
    s3 = signal.sawtooth(2 * np.pi * time)  # Signal 3: saw tooth signal

    # Make a signals matrix
    S = np.c_[s1, s2, s3]
    array_print(M=S, min_value=-1, max_value=1, name="signal matrix")

    # Generate gaussian noise
    noise_cf = 0.1
    EPS = noise_cf * np.random.normal(size=S.shape)
    array_print(M=EPS, min_value=-noise_cf, max_value=noise_cf, name="gaussian noise matrix")

    # Add noise to signals matrix
    S += EPS
    array_print(M=S, min_value=-1.5, max_value=1.5, name="noisy signal matrix")

    # Standardize signals matrix
    S = standardize(S)
    array_print(M=S, min_value=-1.5, max_value=1.5, name="standardized noisy signal matrix")

    return S


def transform(X, EPS, MIN_SAMPLES):
    to_label = dict()

    distance_matrix = 1 - np.corrcoef(X)
    # print(distance_matrix)

    # test: DBSCAN(eps=0.1, min_samples=2, metric='precomputed')
    # solve: DBSCAN(eps=0.0005, min_samples=5, metric='precomputed')
    # prod: DBSCAN(eps=0.02, min_samples=3, metric='precomputed')

    dbscan = DBSCAN(eps=EPS, min_samples=MIN_SAMPLES, metric='precomputed')
    labels = dbscan.fit_predict(distance_matrix)

    for i in range(X.shape[0]):
        to_label[np.array2string(X[i])] = labels[i]

    X_ = list(enumerate(X))

    X_.sort(key=lambda x: to_label[np.array2string(x[1])])

    res = [list(t) for t in zip(*X_)]

    return res[0], np.array(res[1])


def transform_by_permutation(X, permutation):
    X_ = list()
    for i in permutation:
        X_.append(X[i])
    return np.array(X_)


def test():
    xarr = np.array([
        [1.0, 2.0, 3.0],
        [-3.0, 2.0, 1.0],
        [1.0, 2.0, 3.0],
        [-1.0, -2.0, -3.0],
        [1.0, 2.0, 3.0],
        [-1.0, -2.0, -3.0],
        [-3.0, 2.0, 1.0],
        [-1.0, -2.0, -3.0],
        [1.0, 2.0, 3.0],
        [-3.0, 2.0, 1.0],
        [1.0, 2.0, 3.0],
        [-1.0, -2.0, -3.0],
        [-3.0, 2.0, 1.0],
        [-1.0, -2.0, -3.0],
        [1.0, 2.0, 3.0],
        [-1.0, -2.0, -3.0],
        [-3.0, 2.0, 1.0],
        [-1.0, -2.0, -3.0],
        [1.0, 2.0, 3.0],
        [-1.0, -2.0, -3.0]
    ])

    array_print(M=xarr, min_value=-3, max_value=3, name="random")
    print(xarr)

    xarr1, xarr2 = transform(xarr, EPS=0.1, MIN_SAMPLES=2)

    print(xarr1)

    array_print(M=xarr2, min_value=-3, max_value=3, name="random_sort")


def data_read(filename, out_x, out_y):
    # Считывание входных данных
    x = list(list())
    y = list()
    with open(filename, 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter='\t')
        for row in plots:
            y.append(row[0])
            x.append(row[1:])

    X = np.array(x).astype(float)
    Y = np.array(y).astype(str)
    np.savetxt(out_x, X, fmt='%0.8f')
    np.savetxt(out_y, Y, fmt='%s')

    return X


def save_txt(X, out_x):
    np.savetxt(out_x, X, fmt='%0.8f')


def solve():
    # #############################################################################

    # Get signals matrix
    S = signals_gen()

    # Make a mixing matrix
    A = np.array([[1.0, 1.0, 1.0], [0.5, 2.0, 1.0], [1.5, 1.0, 2.0], [2.0, 1.0, 1.0]])
    A = A.T
    np.savetxt('images_{x}/mixing_matrix.txt'.format_map({'x': episode_num}), A, fmt='%0.4f')
    print(S.shape, A.shape)

    # Compute observations matrix
    X = np.matmul(S, A)
    print(X.shape)
    array_print(M=X, name="computed observation matrix")

    # Generate gaussian noise
    noise_cf = 0.1
    X_EPS = noise_cf * np.random.normal(size=X.shape)
    array_print(M=X_EPS, min_value=-noise_cf, max_value=noise_cf, name="gaussian noise matrix")

    # Add noise to observations matrix
    X += X_EPS
    array_print(M=X, name="noisy observation matrix")

    calculate_and_plot(X=X, n_components=3, S=S)


def solve2():
    S = data_read('input_data/test_small.ans_025_cont', 'in_sig_data.txt', 'in_sig_label.txt')
    array_print(M=S, name="true signal matrix")

    X = data_read('input_data/test_small_025.mtx', 'in_obs_data.txt', 'in_obs_label.txt')
    array_print(M=X, name="true observation matrix")

    ANS = data_read('input_data/test_small_025.ans', 'in_ans_data.txt', 'in_ans_label.txt')
    array_print(M=ANS, name="answer")

    calculate_and_plot(X=X, n_components=4, S=S, w=True, ANS=ANS)


def calculate_and_plot(X, n_components, S=None, w=None, ANS=None):
    # #############################################################################

    # Compute ICA
    ica = FastICA(n_components=n_components)

    # Reconstruct signals
    S_ = ica.fit_transform(X)
    array_print(M=S_, name="reconstructed signals")

    # Get estimated mixing matrix
    A_ = ica.mixing_
    A_ = A_.T

    # Compute estimated observations matrix
    X_ = np.matmul(S_, A_) + ica.mean_
    array_print(M=X_, name="reconstructed observation matrix")

    if S is None:
        S = S_

    if w is not None:
        S = standardize(S)
        S_ = standardize(S_)

    S_1, S_2 = transform(S_, EPS=0.03, MIN_SAMPLES=3)
    array_print(M=S_2, name="clustered reconstructed signals")
    save_txt(S_2, "out_signal_reconstruct.txt")

    S2 = transform_by_permutation(S, S_1)
    array_print(M=S2, name="clustered true signals")
    save_txt(S2, "out_signal_true.txt")

    if ANS is not None:
        ANS2 = transform_by_permutation(ANS, S_1)
        array_print(M=ANS2, name="clustered true answer")
        save_txt(ANS2, "out_signal_true_ans.txt")

        EPS_ANS = 0.5

        MY_ANS = np.array(abs(S_2) > EPS_ANS).astype(float)
        array_print(M=MY_ANS, name="clustered reconstruct answer")
        save_txt(MY_ANS, "out_signal_reconstruct_ans.txt")

    # #############################################################################

    # For comparison, compute PCA
    pca = PCA(n_components=n_components)

    # Reconstruct signals based on orthogonal components
    H = pca.fit_transform(X)

    # #############################################################################
    # Plot results
    global img_cnt

    plt.figure()

    models = [X, S, S_, H]
    names = [
        "Observations (mixed signal)",
        "True Sources",
        "ICA recovered signals",
        "PCA recovered signals",
    ]
    colors = ["red", "steelblue", "orange", "green"]

    for ii, (model, name) in enumerate(zip(models, names), 1):
        plt.subplot(4, 1, ii)
        plt.title(name)
        for sig, color in zip(model.T, colors):
            plt.plot(sig, color=color)

    plt.tight_layout()
    img_cnt += 1
    plt.savefig('images_{x}/frame_{y}.png'.format_map({'x': episode_num, 'y': img_cnt}))
    plt.show()

    # Plot results

    models = [X, S, S_, H]
    names = [
        "Observations (mixed signal)",
        "True Sources",
        "ICA recovered signals",
        "PCA recovered signals",
    ]
    colors = ["red", "steelblue", "orange", "green"]

    for ii, (model, name) in enumerate(zip(models, names), 1):
        plt.title(name)
        for sig, color in zip(model.T, colors):
            plt.plot(sig, color=color)
        img_cnt += 1
        plt.savefig('images_{x}/frame_{y}.png'.format_map({'x': episode_num, 'y': img_cnt}))
        plt.show()


if __name__ == '__main__':
    solve2()
