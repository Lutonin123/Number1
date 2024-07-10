package com.example.demo;

import org.springframework.http.*;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.springframework.beans.factory.annotation.Value;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.*;
import java.io.File;
import java.io.IOException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import java.util.List;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import java.util.ArrayList;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Base64;

@Service
public class GitService {

	@Value("${github.login}")
	private String githubLogin;

	@Value("${github.token}")
	private String githubToken;

	@Value("${bitbucket.login}")
	private String bitbucketLogin;

	@Value("${bitbucket.password}")
	private String bitbucketPassword;

	@Value("${bitbucket.workspace}")
	private String bitbucketWorkspace;

	public Mono<String> getReposGitHub() {
		WebClient webClient = WebClient.create("https://api.github.com/user/repos");
		return webClient.get()
				.header("Authorization", "Bearer " + githubToken)
				.header("X-GitHub-Api-Version", "2022-11-28")
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.bodyToMono(String.class);
	}
	
	public Mono<String> getReposBitbucket() {
		String auth = bitbucketLogin + ":" + bitbucketPassword;
		String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
		WebClient webClient = WebClient.create("https://api.bitbucket.org/2.0/repositories/" + bitbucketWorkspace);
		return webClient.get()
				.header("Authorization", "Basic " + encodedAuth)
				.header("Content-Type", "application/json")
				.header("Accept", "application/json")
				.retrieve()
				.bodyToMono(String.class);
	}

	public String updateLocalRepo(String remoteUrl, String localPath, String service) throws IOException, GitAPIException {
		Git git = null;
		Repository repository = null;
		try {
			if (service.toLowerCase().equals("github")) {
				Git.lsRemoteRepository()
					.setHeads(true)
					.setTags(true)
					.setRemote(remoteUrl)
					.setCredentialsProvider(new UsernamePasswordCredentialsProvider(githubLogin, githubToken))
					.call();
			} else {
				Git.lsRemoteRepository()
					.setHeads(true)
					.setTags(true)
					.setRemote(remoteUrl)
					.setCredentialsProvider(new UsernamePasswordCredentialsProvider(bitbucketLogin, bitbucketPassword))
					.call();
			}
			File localDir = new File(localPath);
			int lastSlashIndex = remoteUrl.lastIndexOf("/");
			String repoName = remoteUrl.substring(lastSlashIndex + 1);
			if (!localDir.exists() || !(new File(localDir, ".git").exists() || new File(localDir + "/" + repoName, ".git").exists())) {
				if (service.toLowerCase().equals("github")) {
					git = Git.cloneRepository()
							.setURI(remoteUrl)
							.setDirectory(new File(localPath + "/" + repoName))
							.setCloneAllBranches(true)
							.setCredentialsProvider(new UsernamePasswordCredentialsProvider(githubLogin, githubToken))
							.call();
				} else {
					git = Git.cloneRepository()
							.setURI(remoteUrl)
							.setDirectory(new File(localPath + "/" + repoName))
							.setCloneAllBranches(true)
							.setCredentialsProvider(new UsernamePasswordCredentialsProvider(bitbucketLogin, bitbucketPassword))
							.call();
				}
				List<Ref> branchList = git.branchList().setListMode(ListMode.REMOTE).call();
				for (Ref ref : branchList) {
					String branchName = ref.getName();
					if (branchName.startsWith("refs/remotes/origin/HEAD")) {
						continue;
					}
					if (branchName.startsWith("refs/remotes/origin/master")) {
						continue;
					}
					if (branchName.startsWith("refs/remotes/origin/main")) {
						continue;
					}
					git.checkout()
						.setCreateBranch(true)
						.setName(branchName.replace("refs/remotes/origin/", ""))
						.setStartPoint(branchName)
						.call();
				}
				repository = git.getRepository();
				return "Репозиторий клонирован";
			} else {
				if (new File(localDir + "/" + repoName, ".git").exists()) {
					localDir = new File(localDir + "/" + repoName);
				}
				git = Git.open(localDir);
				List<Ref> branchList = git.branchList().call();
				for (Ref branch : branchList) {
					String branchName = branch.getName();
					if (!branchName.startsWith("refs/heads/")) {
						continue;
					}
					branchName = branchName.substring("refs/heads/".length());
					git.checkout().setName(branchName).call();
					if (service.toLowerCase().equals("github")) {
						git.pull()
							.setCredentialsProvider(new UsernamePasswordCredentialsProvider(githubLogin, githubToken))
							.setRemote("https://github.com/" + githubLogin + "/" + repoName)
							.call();
					} else {
						git.pull()
							.setCredentialsProvider(new UsernamePasswordCredentialsProvider(bitbucketLogin, bitbucketPassword))
							.setRemote("https://bitbucket.org/" + bitbucketWorkspace + "/" + repoName)
							.call();
					}
				}
				return "Локальный репозиторий обновлён";
			}
		} catch (TransportException e) {
			return "Ошибка: Удаленный репозиторий не существует или недоступен";
		} catch (Exception e) {
			return "Ошибка: " + e.getMessage();
		} finally {
			if (git != null) {
				git.close();
			}
			if (repository != null) {
				repository.close();
			}
		}
	}

	public String updateRemoteRepo(String localPath, String service) throws IOException, GitAPIException, InterruptedException {
		Git git = null;
		Repository repository = null;
		int lastSlashIndex = localPath.lastIndexOf("/");
		String repoName = localPath.substring(lastSlashIndex + 1);
		try {
			if (service.toLowerCase().equals("github")) {
				Git.lsRemoteRepository()
					.setHeads(true)
					.setTags(true)
					.setRemote("https://github.com/" + githubLogin + "/" + repoName)
					.setCredentialsProvider(new UsernamePasswordCredentialsProvider(githubLogin, githubToken))
					.call();
			} else {
				Git.lsRemoteRepository()
					.setHeads(true)
					.setTags(true)
					.setRemote("https://bitbucket.org/" + bitbucketWorkspace + "/" + repoName)
					.setCredentialsProvider(new UsernamePasswordCredentialsProvider(bitbucketLogin, bitbucketPassword))
					.call();
			}
			return "Репозиторий есть";
		} catch (TransportException e) {
			if (service.toLowerCase().equals("github")) {
				WebClient webClient = WebClient.create("https://api.github.com/user/repos");
				String requestBody = String.format("{\"name\":\"%s\", \"private\": true}", repoName);
				webClient.post()
						.header("Authorization", "Bearer " + githubToken)
						.header("Content-Type", "application/json")
						.header("Accept", "application/vnd.github.v3+json")
						.bodyValue(requestBody)
						.retrieve()
						.bodyToMono(String.class)
						.subscribe();
			} else {
				String auth = bitbucketLogin + ":" + bitbucketPassword;
				String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
				WebClient webClient = WebClient.create("https://api.bitbucket.org/2.0/repositories/"+ bitbucketWorkspace + "/" + repoName.toLowerCase());
				String requestBody = String.format("{\"scm\": \"git\", \"is_private\": true}");
				webClient.post()
						.header("Authorization", "Basic " + encodedAuth)
						.header("Content-Type", "application/json")
						.header("Accept", "application/json")
						.bodyValue(requestBody)
						.retrieve()
						.bodyToMono(String.class)
						.subscribe();
			}
			return "Репозиторий создан";
		} catch (WebClientResponseException ex) {
			return "Ошибка: " + ex.getMessage();
		} catch (Exception e) {
			return "Ошибка: " + e.getMessage();
		} finally {
			try {
				git = Git.open(new File(localPath));
				List<Ref> branches = git.branchList().call();
				int masterIndex = -1;
				for (int i = 0; i < branches.size(); i++) {
					if (branches.get(i).getName().equals("refs/heads/master")|| branches.get(i).getName().equals("refs/heads/main")) {
						masterIndex = i;
						break;
					}
				}
				if (masterIndex != -1) {
					Ref masterBranch = branches.remove(masterIndex);
					branches.add(0, masterBranch);
				}
				for (Ref branch : branches) {
					String branchName = branch.getName();
					git.checkout()
						.setName(branchName)
						.call();
					if (service.toLowerCase().equals("github")) {
						git.push()
							.setCredentialsProvider(new UsernamePasswordCredentialsProvider(githubLogin, githubToken))
							.setRemote("https://github.com/" + githubLogin + "/" + repoName)
							.add(branchName)
							.call();
					} else {
						git.push()
							.setCredentialsProvider(new UsernamePasswordCredentialsProvider(bitbucketLogin, bitbucketPassword))
							.setRemote("https://bitbucket.org/" + bitbucketWorkspace + "/" + repoName)
							.add(branchName)
							.call();
					}
				}
			} catch (GitAPIException ex) {
				String request = "http://localhost:8080/git/updateRemoteRepo?localPath=" + localPath + "&service="
						+ service;
				WebClient webClient = WebClient.create(request);
				webClient.post()
						.retrieve()
						.bodyToMono(String.class)
						.subscribe();
				return "Репозиторий " + localPath + " создан";
			} finally {
				if (git != null) {
					git.close();
				}
				if (repository != null) {
					repository.close();
				}
			}
		}
	}

	public String updateAllRemoteRepo(String localPath, String service)
			throws IOException, GitAPIException, InterruptedException {
		List<String> folderNames = new ArrayList<>();
		File directory = new File(localPath);
		File[] files = directory.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isDirectory()) {
					folderNames.add(file.getName());
				}
			}
		} else {
			System.out.println("Указанный путь не является директорией или не существует.");
		}
		String repoPatch;
		for (String name : folderNames) {
			repoPatch = localPath + "/" + name;
			updateRemoteRepo(repoPatch, service);
		}
		return "Удалённые репозитории обновлены ";
	}

	public void getReposToFile(String service) {
		Mono<String> responseMono = null;
		if (service.toLowerCase().equals("github")) {
			WebClient webClient = WebClient.create("https://api.github.com/user/repos");
			responseMono = webClient.get()
					.header("Authorization", "Bearer " + githubToken)
					.header("X-GitHub-Api-Version", "2022-11-28")
					.accept(MediaType.APPLICATION_JSON)
					.retrieve()
					.bodyToMono(String.class);
		} else {
			String auth = bitbucketLogin + ":" + bitbucketPassword;
			String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
			WebClient webClient = WebClient.create("https://api.bitbucket.org/2.0/repositories/" + bitbucketWorkspace);
			responseMono = webClient.get()
					.header("Authorization", "Basic " + encodedAuth)
					.header("Content-Type", "application/json")
					.header("Accept", "application/json")
					.retrieve()
					.bodyToMono(String.class);
		}
		responseMono.subscribe(responseBody -> {
			try {
				Path filePath = Paths.get("response.txt");
				Files.write(filePath, responseBody.getBytes(), StandardOpenOption.CREATE);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	public String updateAllLocalRepo(String localPath, String service) throws IOException, GitAPIException, InterruptedException {
		File file = new File("response.txt");
		if (file.exists()) {
			String content = new String(Files.readAllBytes(Paths.get("response.txt")), StandardCharsets.UTF_8);
			if (content.isEmpty()) {
				getReposToFile(service);
				String request = "http://localhost:8080/git/updateAllLocalRepo?localPath=" + localPath + "&service="+ service;
				WebClient webClient = WebClient.create(request);
				webClient.post()
						.retrieve()
						.bodyToMono(String.class)
						.subscribe();
				return "Информации о репозиториях нет";
			} else {
				ArrayList<String> repos = new ArrayList<>();
				if (service.toLowerCase().equals("github")) {
					String regex = "\"name\":\"([^\"]+)\"";
					Pattern pattern = Pattern.compile(regex);
					Matcher matcher = pattern.matcher(content);
					while (matcher.find()) {
						repos.add(matcher.group(1));
					}
				} else {
					String regex = "\"full_name\": \"" + bitbucketWorkspace + "/" + "([^\"]+)\"";
					Pattern pattern = Pattern.compile(regex);
					Matcher matcher = pattern.matcher(content);
					while (matcher.find()) {
						repos.add(matcher.group(1));
					}
				}
				for (String name : repos) {
					if (service.toLowerCase().equals("github"))
						updateLocalRepo("https://github.com/" + githubLogin + "/" + name, localPath, service);
					else
						updateLocalRepo("https://bitbucket.org/" + bitbucketWorkspace + "/" + name, localPath, service);
				}
				file = new File("response.txt");
				file.delete();
				return "Локальные репозитории обновлены";
			}
		} else {
			getReposToFile(service);
			String request = "http://localhost:8080/git/updateAllLocalRepo?localPath=" + localPath + "&service="+ service;
			WebClient webClient = WebClient.create(request);
			webClient.post().retrieve().bodyToMono(String.class).subscribe();
			return "Информации о репозиториях нет";
		}
	}
	
	public String transfer(String from, String to) throws IOException, GitAPIException, InterruptedException {
		if(!from.toLowerCase().equals("github")&&!from.toLowerCase().equals("bitbucket"))
			return "Ошибка в названии сервиса from";
		if(!to.toLowerCase().equals("github")&&!to.toLowerCase().equals("bitbucket"))
			return "Ошибка в названии сервиса to";
		String directoryPath = "reposToTransfer";
		File directory = new File(directoryPath);
		if (!directory.exists()) 
            directory.mkdirs();
		else {
			clearDirectory(directory);
			directory.mkdirs();
		}
		updateAllLocalRepo(directory.getAbsolutePath(),from);
		updateAllRemoteRepo(directory.getAbsolutePath(),to);
		return "Репозитории перенесены из "+from+" в "+to;
	}
	
	private void clearDirectory(File directory) {
		File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                	clearDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
	}
		
}
