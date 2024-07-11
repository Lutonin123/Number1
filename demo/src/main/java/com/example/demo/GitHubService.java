package com.example.demo;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.springframework.beans.factory.annotation.Value;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.*;
import java.io.File;
import java.io.IOException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import java.util.List;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import java.util.ArrayList;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GitHubService {

	@Value("${github.login}")
	private String githubLogin;

	@Value("${github.token}")
	private String githubToken;

	private WebClient webClient = WebClient.create();

	public Mono<String> getReposGitHub() {
		return GitStaticMethods.getReposFromGitHub(githubToken);
	}

	public String updateLocalRepo(String remoteUrl, String localPath) throws IOException, GitAPIException {
		Git git = null;
		Repository repository = null;
		try {
			GitStaticMethods.checkRemoteRepo(remoteUrl, githubLogin, githubToken);
			File localDir = new File(localPath);
			int lastSlashIndex = remoteUrl.lastIndexOf("/");
			String repoName = remoteUrl.substring(lastSlashIndex + 1);
			if (!localDir.exists()
					|| !(new File(localDir, ".git").exists() || new File(localDir + "/" + repoName, ".git").exists())) {
				git = cloneAndUpdateLocalRepo(git, remoteUrl, localPath, repoName);
				repository = git.getRepository();
				return "Локальный репозиторий клонирован";
			} else {
				git = updateExistingLocalRepo(git, remoteUrl, localDir, repoName);
				repository = git.getRepository();
				return "Локальный репозиторий обновлён";
			}
		} finally {
			if (git != null)
				git.close();
			if (repository != null)
				repository.close();
		}
	}

	public Git updateExistingLocalRepo(Git git, String remoteUrl, File localDir, String repoName)
			throws GitAPIException, IOException {
		if (new File(localDir + "/" + repoName, ".git").exists())
			localDir = new File(localDir + "/" + repoName);
		git = Git.open(localDir);
		List<Ref> branchList = git.branchList().call();
		switchOrigin(git, repoName);
		for (Ref branch : branchList) {
			String branchName = branch.getName();
			if (!branchName.startsWith("refs/heads/"))
				continue;
			branchName = branchName.substring("refs/heads/".length());
			git.checkout().setName(branchName).call();
			git.pull().setCredentialsProvider(new UsernamePasswordCredentialsProvider(githubLogin, githubToken))
					.setRemote("origin").call();
		}
		return git;
	}

	public Git cloneAndUpdateLocalRepo(Git git, String remoteUrl, String localPath, String repoName)
			throws GitAPIException {
		git = Git.cloneRepository().setURI(remoteUrl).setDirectory(new File(localPath + "/" + repoName))
				.setCloneAllBranches(true)
				.setCredentialsProvider(new UsernamePasswordCredentialsProvider(githubLogin, githubToken)).call();
		List<Ref> branchList = git.branchList().setListMode(ListMode.REMOTE).call();
		for (Ref ref : branchList) {
			String branchName = ref.getName();
			if (branchName.startsWith("refs/remotes/origin/HEAD"))
				continue;
			if (branchName.startsWith("refs/remotes/origin/master"))
				continue;
			if (branchName.startsWith("refs/remotes/origin/main"))
				continue;
			git.checkout().setCreateBranch(true).setName(branchName.replace("refs/remotes/origin/", ""))
					.setStartPoint(branchName).call();
		}
		return git;
	}

	public String updateRemoteRepo(String localPath) throws IOException, GitAPIException, InterruptedException {
		Git git = null;
		int lastSlashIndex = localPath.lastIndexOf("/");
		String repoName = localPath.substring(lastSlashIndex + 1);
		try {
			String remoteUrl = "https://github.com/" + githubLogin + "/" + repoName;
			GitStaticMethods.checkRemoteRepo(remoteUrl, githubLogin, githubToken);
			return "Репозиторий есть";
		} catch (TransportException e) {
			createRemoteRepo(repoName);
			return "Репозиторий создан";
		} finally {
			tryUpdateRemoteRepo(git, localPath, repoName);
		}
	}

	public void createRemoteRepo(String repoName) {
		String requestBody = String.format("{\"name\":\"%s\", \"private\": true}", repoName);
		webClient.post().uri("https://api.github.com/user/repos").header("Authorization", "Bearer " + githubToken)
				.header("Content-Type", "application/json").header("Accept", "application/vnd.github.v3+json")
				.bodyValue(requestBody).retrieve().bodyToMono(String.class).subscribe();
	}

	public void tryUpdateRemoteRepo(Git git, String localPath, String repoName) throws IOException {
		try {
			git = Git.open(new File(localPath));
			List<Ref> branches = git.branchList().call();
			int masterIndex = -1;
			for (int i = 0; i < branches.size(); i++) {
				if (branches.get(i).getName().equals("refs/heads/master")
						|| branches.get(i).getName().equals("refs/heads/main")) {
					masterIndex = i;
					break;
				}
			}
			if (masterIndex != -1) {
				Ref masterBranch = branches.remove(masterIndex);
				branches.add(0, masterBranch);
			}
			switchOrigin(git, repoName);
			for (Ref branch : branches) {
				String branchName = branch.getName();
				git.checkout().setName(branchName).call();
				git.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider(githubLogin, githubToken))
						.setRemote("origin").add(branchName).call();
			}
		} catch (GitAPIException ex) {
			webClient.post().uri("http://localhost:8080/gitHub/updateRemoteRepo?localPath=" + localPath).retrieve()
					.bodyToMono(String.class).subscribe();
		} finally {
			Repository repository = git.getRepository();
			if (git != null)
				git.close();
			if (repository != null)
				repository.close();
		}
	}

	public String updateAllRemoteRepo(String localPath) throws IOException, GitAPIException, InterruptedException {
		List<String> folderNames = new ArrayList<>();
		File directory = new File(localPath);
		File[] files = directory.listFiles();
		if (files != null)
			for (File file : files)
				if (file.isDirectory())
					folderNames.add(file.getName());
				else
					return "Указанный путь не является директорией или не существует.";
		String repoPatch;
		for (String name : folderNames) {
			repoPatch = localPath + "/" + name;
			updateRemoteRepo(repoPatch);
		}
		return "Удалённые репозитории обновлены ";
	}

	public String updateAllLocalRepo(String localPath) throws IOException, GitAPIException, InterruptedException {
		File file = new File("response.txt");
		if (file.exists()) {
			String content = new String(Files.readAllBytes(Paths.get("response.txt")), StandardCharsets.UTF_8);
			if (content.isEmpty()) {
				GitStaticMethods.getReposToFile(GitStaticMethods.getReposFromGitHub(githubToken));
				return "Перезапуск";
			} else {
				ArrayList<String> repos = new ArrayList<>();
				String regex = "\"name\":\"([^\"]+)\"";
				Pattern pattern = Pattern.compile(regex);
				Matcher matcher = pattern.matcher(content);
				while (matcher.find())
					repos.add(matcher.group(1));
				for (String name : repos)
					updateLocalRepo("https://github.com/" + githubLogin + "/" + name, localPath);
				file = new File("response.txt");
				file.delete();
				return "Локальные репозитории обновлены";
			}
		} else {
			GitStaticMethods.getReposToFile(GitStaticMethods.getReposFromGitHub(githubToken));
			return "Перезапуск";
		}
	}

	public String transfer() throws IOException, GitAPIException, InterruptedException {
		String directoryPath = "reposToTransfer";
		File directory = new File(directoryPath);
		if (!directory.exists())
			directory.mkdirs();
		else {
			GitStaticMethods.clearDirectory(directory);
			directory.mkdirs();
		}
		updateAllLocalRepo(directory.getAbsolutePath());
		webClient.post().uri("http://localhost:8080/bitbucket/updateAllRemoteRepo?localPath=" + directory.getAbsolutePath()).retrieve().bodyToMono(String.class).subscribe();
		return "Репозитории перенесены из GitHub в Bitbucket";
	}

	public void switchOrigin(Git git, String repoName) throws IOException {
		StoredConfig config = git.getRepository().getConfig();
		config.setString("remote", "origin", "url", "https://github.com/" + githubLogin + "/" + repoName);
		config.save();
	}
}
