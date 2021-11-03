import numpy as np
import matplotlib.pyplot as plt
from scipy import signal

from sklearn.decomposition import FastICA, PCA


def array_print(M, min_value=None, max_value=None):
    shape = np.array(M.shape)
    print(shape)
    plt.imshow(M, interpolation="nearest")
    if (min_value is not None) and (max_value is not None):
        plt.clim(vmin=min_value, vmax=max_value)
    plt.colorbar()
    plt.xlim(-1, shape[1])
    plt.ylim(-1, shape[0])
    plt.gca().set_aspect('auto', adjustable='box')
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
    array_print(M=S, min_value=-1, max_value=1)

    # Generate gaussian noise
    noise_cf = 0.2
    EPS = noise_cf * np.random.normal(size=S.shape)
    array_print(M=EPS, min_value=-noise_cf, max_value=noise_cf)

    # Add noise to signals matrix
    S += EPS
    array_print(M=S, min_value=-1.5, max_value=1.5)

    # Standardize signals matrix
    st_dev = S.std(axis=0)
    print("standard deviation:", st_dev)
    S /= st_dev
    array_print(M=S, min_value=-1.5, max_value=1.5)

    return S


def solve():
    # #############################################################################

    # Get signals matrix
    S = signals_gen()

    # Make a mixing matrix
    A = np.array([[1.0, 1.0, 1.0], [0.5, 2.0, 1.0], [1.5, 1.0, 2.0], [2.0, 1.0, 1.0]])
    print(S.shape, A.T.shape)

    # Compute observations matrix
    X = np.matmul(S, A.T)
    print(X.shape)
    array_print(M=X)

    # Generate gaussian noise
    noise_cf = 0.2
    X_EPS = noise_cf * np.random.normal(size=X.shape)
    array_print(M=X_EPS, min_value=-noise_cf, max_value=noise_cf)

    # Add noise to observations matrix
    X += X_EPS
    array_print(M=X)

    # #############################################################################

    # Compute ICA
    ica = FastICA(n_components=3)
    S_ = ica.fit_transform(X)  # Reconstruct signals
    A_ = ica.mixing_  # Get estimated mixing matrix

    # We can `prove` that the ICA model applies by reverting the unmixing.
    # assert np.allclose(X, np.dot(S_, A_.T) + ica.mean_)

    # For comparison, compute PCA
    pca = PCA(n_components=3)
    H = pca.fit_transform(X)  # Reconstruct signals based on orthogonal components

    # #############################################################################
    # Plot results

    plt.figure()

    models = [X, S, S_, H]
    names = [
        "Observations (mixed signal)",
        "True Sources",
        "ICA recovered signals",
        "PCA recovered signals",
    ]
    colors = ["red", "steelblue", "orange"]

    for ii, (model, name) in enumerate(zip(models, names), 1):
        plt.subplot(4, 1, ii)
        plt.title(name)
        for sig, color in zip(model.T, colors):
            plt.plot(sig, color=color)

    plt.tight_layout()
    plt.show()


if __name__ == '__main__':
    solve()
