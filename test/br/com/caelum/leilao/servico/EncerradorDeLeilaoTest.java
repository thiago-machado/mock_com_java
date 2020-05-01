package br.com.caelum.leilao.servico;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.any;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import br.com.caelum.leilao.builder.CriadorDeLeilao;
import br.com.caelum.leilao.dominio.Leilao;
import br.com.caelum.leilao.infra.dao.EnviadorDeEmail;
import br.com.caelum.leilao.infra.dao.RepositorioDeLeiloes;

/**
 * IMPORTANTE: � imposs�vel mockar m�todos est�ticos!
 * 
 * Por esse motivo, desenvolvedores fan�ticos por testes evitam ao m�ximo criar
 * m�todos est�ticos!
 * 
 * Fuja deles! Al�m de serem dif�ceis de serem testados, ainda n�o s�o uma boa
 * pr�tica de orienta��o a objetos.
 */
public class EncerradorDeLeilaoTest {

	private EnviadorDeEmail carteiro;

	@Before
	public void configuracaoInicial() {
		carteiro = mock(EnviadorDeEmail.class);
	}

	@Test
	public void deveEncerrarLeiloesQueComecaramUmaSemanaAtras() {

		Calendar antiga = Calendar.getInstance();
		antiga.set(1999, 1, 20);

		Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma").naData(antiga).constroi();
		Leilao leilao2 = new CriadorDeLeilao().para("Geladeira").naData(antiga).constroi();
		List<Leilao> leiloesAntigos = Arrays.asList(leilao1, leilao2);

		/*
		 * Criando o Mock
		 */
		RepositorioDeLeiloes dao = mock(RepositorioDeLeiloes.class);

		/*
		 * Ensinando o mock a reagir da maneira que esperamos. Toda vez que DAO chamar
		 * .correntes(), ele retornar� nosso List<Leilao> leiloesAntigos
		 */
		when(dao.correntes()).thenReturn(leiloesAntigos);

		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(dao, carteiro);

		/*
		 * Esse m�todo, internamente, executar� a fun��o dao.correntes(). Como estamos
		 * usando o Mock, sempre retornar� nosso List<Leilao> leiloesAntigos
		 */
		encerrador.encerra();

		/*
		 * Busca no banco a lista de encerrados
		 */
		List<Leilao> encerrados = dao.encerrados();

		assertTrue(leilao1.isEncerrado());
		assertTrue(leilao2.isEncerrado());
		assertEquals(2, encerrador.getTotalEncerrados()); // vamos conferir tambem o tamanho da lista!
	}

	@Test
	public void naoDeveEncerrarLeiloesQueComecaramMenosDeUmaSemanaAtras() {

		Calendar ontem = Calendar.getInstance();
		ontem.add(Calendar.DAY_OF_MONTH, -1);

		Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma").naData(ontem).constroi();
		Leilao leilao2 = new CriadorDeLeilao().para("Geladeira").naData(ontem).constroi();

		RepositorioDeLeiloes dao = mock(RepositorioDeLeiloes.class);
		when(dao.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));

		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(dao, carteiro);
		encerrador.encerra();

		assertEquals(0, encerrador.getTotalEncerrados());
		assertFalse(leilao1.isEncerrado());
		assertFalse(leilao2.isEncerrado());

		/*
		 * Verificando que o m�todo atualiza() nunca foi executado.
		 */
		verify(dao, never()).atualiza(leilao1);
		verify(dao, never()).atualiza(leilao2);
	}

	@Test
	public void naoDeveEncerrarLeiloesCasoNaoHajaNenhum() {

		RepositorioDeLeiloes dao = mock(RepositorioDeLeiloes.class);
		when(dao.correntes()).thenReturn(new ArrayList<Leilao>());

		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(dao, carteiro);
		encerrador.encerra();

		assertEquals(0, encerrador.getTotalEncerrados());
	}

	@Test
	public void deveAtualizarLeiloesEncerrados() {

		Calendar antiga = Calendar.getInstance();
		antiga.set(1999, 1, 20);

		Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma").naData(antiga).constroi();

		RepositorioDeLeiloes dao = mock(RepositorioDeLeiloes.class);
		when(dao.correntes()).thenReturn(Arrays.asList(leilao1));

		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(dao, carteiro);
		encerrador.encerra();

		/*
		 * Verifica se o m�todo atualiza() � chamado dentro da chamada do m�todo
		 * correntes(). Al�m disso, verificamos se o m�todo atualiza � executado somente
		 * 1 vez.
		 * 
		 * Atrav�s do verify(), conseguimos, ent�o, testar que m�todos s�o invocados,
		 * garantindo o comportamento de uma classe por completo.
		 */
		verify(dao, times(1)).atualiza(leilao1);

		/*
		 * Outras formas de utilizar o VERIFY
		 * 
		 * O m�todo atLeastOnce() vai garantir que o m�todo foi invocado no m�nimo uma
		 * vez. Ou seja, se ele foi invocado 1, 2, 3 ou mais vezes, o teste passa. Se
		 * ele n�o for invocado, o teste vai falhar.
		 * 
		 * O m�todo atLeast(numero) funciona de forma an�loga ao m�todo acima, com a
		 * diferen�a de que passamos para ele o n�mero m�nimo de invoca��es que um
		 * m�todo deve ter.
		 * 
		 * Por fim, o m�todo atMost(numero) nos garante que um m�todo foi executado at�
		 * no m�ximo N vezes. Ou seja, se o m�todo tiver mais invoca��es do que o que
		 * foi passado para o atMost, o teste falha.
		 */
	}

	@Test
	public void deveEnviarEmailAposPersistirLeilaoEncerrado() {
		Calendar antiga = Calendar.getInstance();
		antiga.set(1999, 1, 20);

		Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma").naData(antiga).constroi();

		RepositorioDeLeiloes dao = mock(RepositorioDeLeiloes.class);
		when(dao.correntes()).thenReturn(Arrays.asList(leilao1));

		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(dao, carteiro);
		encerrador.encerra();

		/*
		 * InOrder serve para verificar a ordem que os m�todos s�o chamados. Ou seja,
		 * queremos verificar se atuliza() � executado antes da envia().
		 */
		InOrder inOrder = inOrder(dao, carteiro);
		inOrder.verify(dao, times(1)).atualiza(leilao1);
		inOrder.verify(carteiro, times(1)).envia(leilao1);
	}

	@Test
	public void deveContinuarAExecucaoMesmoQuandoDaoFalha() {
		Calendar antiga = Calendar.getInstance();
		antiga.set(1999, 1, 20);

		Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma").naData(antiga).constroi();
		Leilao leilao2 = new CriadorDeLeilao().para("Geladeira").naData(antiga).constroi();

		RepositorioDeLeiloes dao = mock(RepositorioDeLeiloes.class);
		when(dao.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));

		/*
		 * Deve lan�ar exce��o do tipo RuntimeException ao executar o atualiza() com
		 * leilao1. Ou seja, quando executar o m�todo dao.atualiza para leilao1, o
		 * mockito lan�ar� uma exce��o!
		 */
		doThrow(new RuntimeException()).when(dao).atualiza(leilao1);

		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(dao, carteiro);
		encerrador.encerra();

		// times(0) e never() s�o a mesma coisa
		verify(dao).atualiza(leilao2);
		verify(carteiro).envia(leilao2);
		// verifica que o m�todo envia n�o foi executado por causa da exce��o lan�ada
		verify(carteiro, never()).envia(leilao1);
	}

	@Test
	public void deveContinuarAExecucaoMesmoQuandoEnviadorDeEmaillFalha() {
		Calendar antiga = Calendar.getInstance();
		antiga.set(1999, 1, 20);

		Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma").naData(antiga).constroi();
		Leilao leilao2 = new CriadorDeLeilao().para("Geladeira").naData(antiga).constroi();

		RepositorioDeLeiloes dao = mock(RepositorioDeLeiloes.class);
		when(dao.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));

		/*
		 * Mockito lan�ar� exce��o quando carteiro.envia () para leilao 1 for chamado.
		 */
		doThrow(new RuntimeException()).when(carteiro).envia(leilao1);

		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(dao, carteiro);
		encerrador.encerra();

		verify(dao).atualiza(leilao2);
		verify(carteiro).envia(leilao2);
	}

	@Test
	public void deveDesistirSeDaoFalhaPraSempre() {
		Calendar antiga = Calendar.getInstance();
		antiga.set(1999, 1, 20);

		Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma").naData(antiga).constroi();
		Leilao leilao2 = new CriadorDeLeilao().para("Geladeira").naData(antiga).constroi();

		RepositorioDeLeiloes dao = mock(RepositorioDeLeiloes.class);
		when(dao.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));
		
		/*
		 * Mockito lan�ar� exce��o para todas as chamadas de dao.atualiza().
		 */
		doThrow(new RuntimeException()).when(dao).atualiza(any(Leilao.class));

		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(dao, carteiro);

		encerrador.encerra();

		// Verificando que o m�todo carteiro.envia() nunca � executada para qualquer leil�o
		verify(carteiro, never()).envia(any(Leilao.class));
	}
}
