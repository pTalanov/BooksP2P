from subprocess import call
from shutil import rmtree, copytree
import sys
import os
import threading
import time

peer_jar = "Peer.jar"
server_jar = "Server.jar"
generate_book_jar = "BookGenerator.jar"


class ServerThread(threading.Thread):
    def __init__(self, port, seconds_to_run, log_file):
        super(ServerThread, self).__init__()
        self.port = port
        self.seconds_to_run = seconds_to_run
        self.log_file = log_file

    def run(self):
        call(["java", "-jar", server_jar, str(self.port), str(self.seconds_to_run), self.log_file])


class PeerThread(threading.Thread):
    def __init__(self, root, port, server_port, seconds_to_run, log_file, books_to_download):
        super(PeerThread, self).__init__()
        self.root = root
        self.port = port
        self.server_port = server_port
        self.seconds_to_run = seconds_to_run
        self.books_to_download = books_to_download
        self.log_file = log_file

    def run(self):
        call(["java", "-jar", peer_jar, self.root, str(self.port), str(self.server_port), str(self.seconds_to_run),
              self.log_file] + self.books_to_download)


def generate_book(path, chapters_count):
    os.makedirs(path)
    call(["java", "-jar", generate_book_jar, path, str(chapters_count)])


catalogues_dir = "catalogues/"


def catalogue_path(peer_id):
    return catalogues_dir + str(peer_id)


logs_dir = "logs/"

PEERS_COUNT = 12

regenerate_data = True

if regenerate_data:
    if os.path.exists(catalogues_dir):
        rmtree(catalogues_dir)
    if os.path.exists(logs_dir):
        rmtree(logs_dir)
    os.makedirs(logs_dir)

    #first 3 peers
    generate_book(catalogue_path(1) + "/sample1", 100)
    copytree(catalogue_path(1), catalogue_path(2))
    copytree(catalogue_path(1), catalogue_path(3))

    #second 3 peers
    generate_book(catalogue_path(4) + "/sample2", 200)
    copytree(catalogue_path(4), catalogue_path(5))
    copytree(catalogue_path(4), catalogue_path(6))

    #third 3 peers
    generate_book(catalogue_path(7) + "/sample3", 500)
    copytree(catalogue_path(7), catalogue_path(8))
    copytree(catalogue_path(7), catalogue_path(9))

    for i in range(10, 13):
        os.makedirs(catalogue_path(i))

server_port = 57616
ServerThread(server_port, 170, logs_dir + "server.log").start()
time.sleep(2)


def book_interested_in(peer_id):
    if peer_id in range(1, 4):
        return ["sample2"]
    if peer_id in range(4, 7):
        return ["sample3"]
    if peer_id in range(7, 10):
        return ["sample1"]
    return ["sample1", "sample2", "sample3"]


for peer_id in range(1, 13):
    PeerThread(catalogues_dir + str(peer_id), server_port + peer_id * 10, server_port, 150,
               logs_dir + "client" + str(peer_id) + ".log",
               book_interested_in(peer_id)).start()

