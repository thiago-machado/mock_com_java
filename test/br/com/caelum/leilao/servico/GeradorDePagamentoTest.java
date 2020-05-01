package br.com.caelum.leilao.servico;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Calendar;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import br.com.caelum.leilao.builder.CriadorDeLeilao;
import br.com.caelum.leilao.dominio.Leilao;
import br.com.caelum.leilao.dominio.Pagamento;
import br.com.caelum.leilao.dominio.Usuario;
import br.com.caelum.leilao.infra.dao.Relogio;
import br.com.caelum.leilao.infra.dao.RepositorioDeLeiloes;
import br.com.caelum.leilao.infra.dao.RepositorioDePagamentos;

public class GeradorDePagamentoTest {

	private RepositorioDeLeiloes leiloes;
	private RepositorioDePagamentos pagamentos;
	private Avaliador avaliador;
	private Relogio relogio;

	/*
	 * Notar que estamos mockando o Avaliador. Com base nisso, temos a seguinte
	 * quest�o: quando devemos mockar e quando n�o devemos mockar uma classe?
	 * 
	 * Geralmente optamos por mockar classes que s�o dif�ceis de serem testadas. Por
	 * exemplo, se n�o mockarmos um DAO ou uma classe que envia e-mail, dificilmente
	 * conseguiremos testar aquela classe.
	 * 
	 * Classes de dom�nio, como entidades e etc, geralmente n�o necessitam de mocks.
	 * Nesses casos, � at� bom n�o mockarmos, pois se ela tiver algum bug, a chance
	 * de um teste pegar � maior.
	 * 
	 * Ou seja, nesse caso, n�o precisar�amos utilizar um Mock de Avaliador, mas sua
	 * inst�ncia real.
	 */
	@Before
	public void configuracaoInicial() {
		leiloes = mock(RepositorioDeLeiloes.class);
		pagamentos = mock(RepositorioDePagamentos.class);
		avaliador = mock(Avaliador.class);
		relogio = mock(Relogio.class);
	}

	@Test
	public void deveGerarPagamentoParaUmLeilaoEncerrado() {

		Leilao leilao = new CriadorDeLeilao().para("Playstation").lance(new Usuario("Jos� da Silva"), 2000.0)
				.lance(new Usuario("Maria Pereira"), 2500.0).constroi();

		when(leiloes.encerrados()).thenReturn(Arrays.asList(leilao));
		// when(avaliador.getMaiorLance()).thenReturn(2500.0);

		// GeradorDePagamento gerador = new GeradorDePagamento(leiloes, pagamentos,
		// avaliador);
		GeradorDePagamento gerador = new GeradorDePagamento(leiloes, pagamentos, new Avaliador()); // resultado � o
																									// mesmo das linhas
																									// comentadas acima
		gerador.gera();

		/*
		 * O problema est� em como fazer o assert no Pagamento que � gerado pela classe
		 * GeradorDePagamento, afinal, ele � instanciado internamente e n�o temos como
		 * recuper�-lo no nosso m�todo de teste.
		 * 
		 * Mas repare que a inst�ncia � passada para o RepositorioDePagamentos, que � um
		 * mock! Ent�o, podemos pedir ao Mock para guardar esse objeto para que possamos
		 * recuper�-lo � fim de realizar as asser��es! A classe do Mockito que faz isso
		 * � chamada de ArgumentCaptor, ou seja, capturador de argumentos.
		 * 
		 * Para a utilizarmos, precisamos instanci�-la, passando qual a classe ser�
		 * recuperada. Em nosso caso, est� classe � Pagamento. Em seguida, fazemos uso
		 * do verify() e checamos a execu��o do m�todo que recebe o atributo. Como
		 * par�metro, passamos o m�todo capture() do ArgumentCaptor.
		 */
		ArgumentCaptor<Pagamento> argumento = ArgumentCaptor.forClass(Pagamento.class);
		verify(pagamentos).salva(argumento.capture()); // definindo que queremos pegar o argumento Pagamento quando
														// executar pagamentos.salva()

		Pagamento pagamentoGerado = argumento.getValue(); // recuperando a inst�ncia de Pagamento passsada em
															// pagamentos.salva()
		assertEquals(2500.0, pagamentoGerado.getValor(), 0.00001);
	}

	/*
	 * Sempre que tivermos dificuldade de testar algum trecho de c�digo - geralmente
	 * os que fazem uso de m�todos est�ticos (como Calendar, por exemplo), � comum
	 * criarmos abstra��es para facilitar o teste. A abstra��o de rel�gio � muito
	 * comum em sistemas bem testados.
	 * 
	 * Uma �tima dica para se levar �: se est� dif�cil testar, � porque nosso
	 * projeto de classes n�o est� bom o suficiente.
	 * 
	 * Idealmente, deve ser f�cil escrever um teste de unidade. Use seus
	 * conhecimentos de orienta��o a objetos, crie abstra��es, escreva classes
	 * pequenas, diminua o acoplamento... Tudo isso facilitar� o seu teste!
	 */
	@Test
	public void deveEmpurrarParaOProximoDiaUtil() {

		// 25/04/2020 � um S�bado
		Calendar sabado = Calendar.getInstance();
		sabado.set(2020, Calendar.APRIL, 25);

		// ensinamos o mock a dizer que "hoje" � sabado!
		when(relogio.hoje()).thenReturn(sabado);

		Leilao leilao = new CriadorDeLeilao().para("Playstation").lance(new Usuario("Jos� da Silva"), 2000.0)
				.lance(new Usuario("Maria Pereira"), 2500.0).constroi();

		when(leiloes.encerrados()).thenReturn(Arrays.asList(leilao));

		GeradorDePagamento gerador = new GeradorDePagamento(leiloes, pagamentos, new Avaliador(), relogio);
		gerador.gera();

		ArgumentCaptor<Pagamento> argumento = ArgumentCaptor.forClass(Pagamento.class);
		verify(pagamentos).salva(argumento.capture());
		Pagamento pagamentoGerado = argumento.getValue();

		/*
		 * Verificando se o pagamento gerado foi empurrado para uma Segunda-Feira, dia
		 * 27
		 */
		assertEquals(Calendar.MONDAY, pagamentoGerado.getData().get(Calendar.DAY_OF_WEEK));
		assertEquals(27, pagamentoGerado.getData().get(Calendar.DAY_OF_MONTH));
	}

}
