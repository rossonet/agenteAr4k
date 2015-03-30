package org.ar4k

import org.codehaus.groovy.grails.commons.GrailsApplication
import grails.transaction.Transactional
import com.jcraft.jsch.*

@Transactional
class SSHService {
	def GrailsApplication grailsApplication
	def servletContext
	def connessioniSSH = []
	def canaliSSH = []
	def proxySSH = []
	JSch jsch

	// Aggiunge una sessione ed eventualmente istanzia la classe JSch
	def String addConnession(String username, String host, int port, String password ) {
		if (jsch == null) {
			jsch = new JSch()
		}
		Session session=jsch.getSession(username, host, port)
		UserInfo ui=new SSHUserInfo()
		ui.setPassword(password)
		session.setUserInfo(ui)
		session.connect()
		connessioniSSH.add(session)
		return session.getSessionId().encodeAsMD5()
	}

	// Lista le sessioni
	def List<String> listConnession() {
		def lista = []
		connessioniSSH.each{
			lista.add(it.getSessionId().encodeAsMD5().toString())
		}
		return lista
	}

	// ritrova i dati di una sessione per SessionId
	def Session getSession(String ricerca) {
		for(Session sessione in connessioniSSH) {
			if (sessione.getSessionId().encodeAsMD5() == ricerca) {
				return sessione
				break
			}
		}
	}

	// rimuove una sessione per SessionId
	def boolean removeConnession(String ricerca) {
		for(Session sessione in connessioniSSH) {
			if (sessione.getSessionId().encodeAsMD5() == ricerca) {
				sessione.disconnect()
				if (sessione.isConnected)
				{
					return false
				} else {
					def temporanei = []
					for(Session ses in connessioniSSH) {
						if (ses.getSessionId().encodeAsMD5() != ricerca) {
							temporanei.add(ses)
						}
					}
					connessioniSSH = temporanei
					return true
				}
				break
			}
		}
	}

	//Gestisce i tunnel R

	def List<String> listPortForwardingR(String sessione) {
		for(Session ses in connessioniSSH) {
			if (ses.getSessionId().encodeAsMD5() == sessione) {
				return ses.getPortForwardingR()*.toString()
			}
		}
	}

	def boolean addRTunnel(String ricerca, int rport, String lhost, int lport) {
		String risultato
		for(Session sessione in connessioniSSH) {
			//println "Test: "+sessione.getSessionId().encodeAsMD5() +' == ' + ricerca
			if (sessione.getSessionId().encodeAsMD5() == ricerca) {
				sessione.setPortForwardingR(rport, lhost, lport)
				risultato = sessione.isConnected()
				//println "Aperto il canale RTunnel..."
			}
			return risultato
		}
	}

	def boolean removeRTunnel(String ricerca, int rport) {
		String risultato
		for(Session sessione in connessioniSSH) {
			//println "Test: "+sessione.getSessionId().encodeAsMD5() +' == ' + ricerca
			if (sessione.getSessionId().encodeAsMD5() == ricerca) {
				sessione.delPortForwardingR(rport)
				risultato = sessione.isConnected()
				//println "Aperto il canale RTunnel..."
			}
			return risultato
		}
	}

	//Gestisce i tunnel L

	def List<String> listPortForwardingL(String sessione) {
		for(Session ses in connessioniSSH) {
			if (ses.getSessionId().encodeAsMD5() == sessione) {
				return ses.getPortForwardingL()*.toString()
			}
		}
	}

	def boolean addLTunnel(String ricerca, int lport, String rhost, int rport) {
		String risultato
		for(Session sessione in connessioniSSH) {
			//println "Test: "+sessione.getSessionId().encodeAsMD5() +' == ' + ricerca
			if (sessione.getSessionId().encodeAsMD5() == ricerca) {
				sessione.setPortForwardingL(lport, rhost, rport)
				risultato = sessione.isConnected()
				//println "Aperto il canale RTunnel..."
			}
			return risultato
		}
	}

	def boolean removeLTunnel(String ricerca, int lport) {
		String risultato
		for(Session sessione in connessioniSSH) {
			//println "Test: "+sessione.getSessionId().encodeAsMD5() +' == ' + ricerca
			if (sessione.getSessionId().encodeAsMD5() == ricerca) {
				sessione.delPortForwardingL(lport)
				risultato = sessione.isConnected()
				//println "Aperto il canale RTunnel..."
			}
			return risultato
		}
	}

	// Gestisce l'esecuzione di comandi
	def String esegui(String ricerca,String comando, def errore) {
		String risultato = ""
		Channel channel
		for(Session sessione in connessioniSSH) {
			//println "Test: "+sessione.getSessionId().encodeAsMD5() +' == ' + ricerca
			if (sessione.getSessionId().encodeAsMD5() == ricerca) {
				channel=sessione.openChannel("exec")
				((ChannelExec)channel).setCommand(comando)
				channel.setInputStream(null)
				((ChannelExec)channel).setErrStream(errore)
				InputStream input=channel.getInputStream()
				channel.connect()
				byte[] tmp=new byte[1024]
				while(true){
					while(input.available()>0){
						int i=input.read(tmp, 0, 1024)
						if(i<0)break
						risultato += new String(tmp, 0, i)
					}
					if(channel.isClosed()){
						if(input.available()>0) continue
							println("exit-status: "+channel.getExitStatus())
						break
					}
					try{Thread.sleep(1000);}catch(Exception ee){}
				}
				channel.disconnect()
			}
			return risultato
		}
	}

	// connette la console
	def boolean console(String ricerca, def entrata, def uscita) {
		String risultato = ""
		Channel channel
		for(Session sessione in connessioniSSH) {
			//println "Test: "+sessione.getSessionId().encodeAsMD5() +' == ' + ricerca
			if (sessione.getSessionId().encodeAsMD5() == ricerca) {
				channel=sessione.openChannel("shell")
				channel.setOutputStream(uscita)
				channel.setInputStream(entrata)
				channel.connect()
				risultato = sessione.isConnected()
			}
			return risultato
		}
	}

	def byte[] addStream(byte[] sessione) {
		return null
	}

	def byte[] addScpTo(byte[] sessione) {
		return null
	}

	def byte[] addScpFrom(byte[] sessione) {
		return null
	}

	def byte[] addSftp(byte[] sessione) {
		return null
	}
}


public class SSHUserInfo implements UserInfo {
	String passwd

	public void showMessage(String message){
		println message
	}

	public String getPassword() {
		return passwd;
	}

	public void setPassword(String pass) {
		passwd = pass;
	}

	public String getPassphrase(){return null}
	public boolean promptPassphrase(String message){return true} // Per chiedere la chiave di decodifica della password
	public boolean promptPassword(String message) {return true} // Per chiedere la password
	public boolean promptYesNo(String str) {return true} // Per accetare le key
}