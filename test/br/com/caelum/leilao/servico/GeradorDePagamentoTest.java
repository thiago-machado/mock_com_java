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
	 * questão: quando devemos mockar e quando não devemos mockar uma classe?
	 * 
	 * Geralmente optamos por mockar classes que são difíceis de serem testadas. Por
	 * exemplo, se não mockarmos um DAO ou uma classe que envia e-mail, dificilmente
	 * conseguiremos testar aquela classe.
	 * 
	 * Classes de domínio, como entidades e etc, geralmente não necessitam de mocks.
	 * Nesses casos, é até bom não mockarmos, pois se ela tiver algum bug, a chance
	 * de um teste pegar é maior.
	 * 
	 * Ou seja, nesse caso, não precisaríamos utilizar um Mock de Avaliador, mas sua
	 * instância real.
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

		Leilao leilao = new CriadorDeLeilao().para("Playstation").lance(new Usuario("José da Silva"), 2000.0)
				.lance(new Usuario("Maria Pereira"), 2500.0).constroi();

		when(leiloes.encerrados()).thenReturn(Arrays.asList(leilao));
		// when(avaliador.getMaiorLance()).thenReturn(2500.0);

		// GeradorDePagamento gerador = new GeradorDePagamento(leiloes, pagamentos,
		// avaliador);
		GeradorDePagamento gerador = new GeradorDePagamento(leiloes, pagamentos, new Avaliador()); // resultado é o
																									// mesmo das linhas
																									// comentadas acima
		gerador.gera();

		/*
		 * O problema está em como fazer o assert no Pagamento que é gerado pela classe
		 * GeradorDePagamento, afinal, ele é instanciado internamente e não temos como
		 * recuperá-lo no nosso método de teste.
		 * 
		 * Mas repare que a instância é passada para o RepositorioDePagamentos, que é um
		 * mock! Então, podemos pedir ao Mock para guardar esse objeto para que possamos
		 * recuperá-lo à fim de realizar as asserções! A classe do Mockito que faz isso
		 * é chamada de ArgumentCaptor, ou seja, capturador de argumentos.
		 * 
		 * Para a utilizarmos, precisamos instanciá-la, passando qual a classe será
		 * recuperada. Em nosso caso, está classe é Pagamento. Em seguida, fazemos uso
		 * do verify() e checamos a execução do método que recebe o atributo. Como
		 * parâmetro, passamos o método capture() do ArgumentCaptor.
		 */
		ArgumentCaptor<Pagamento> argumento = ArgumentCaptor.forClass(Pagamento.class);
		verify(pagamentos).salva(argumento.capture()); // definindo que queremos pegar o argumento Pagamento quando
														// executar pagamentos.salva()

		Pagamento pagamentoGerado = argumento.getValue(); // recuperando a instância de Pagamento passsada em
															// pagamentos.salva()
		assertEquals(2500.0, pagamentoGerado.getValor(), 0.00001);
	}

	/*
	 * Sempre que tivermos dificuldade de testar algum trecho de código - geralmente
	 * os que fazem uso de métodos estáticos (como Calendar, por exemplo), é comum
	 * criarmos abstrações para facilitar o teste. A abstração de relógio é muito
	 * comum em sistemas bem testados.
	 * 
	 * Uma ótima dica para se levar é: se está difícil testar, é porque nosso
	 * projeto de classes não está bom o suficiente.
	 * 
	 * Idealmente, deve ser fácil escrever um teste de unidade. Use seus
	 * conhecimentos de orientação a objetos, crie abstrações, escreva classes
	 * pequenas, diminua o acoplamento... Tudo isso facilitará o seu teste!
	 */
	@Test
	public void deveEmpurrarParaOProximoDiaUtil() {

		// 25/04/2020 é um Sábado
		Calendar sabado = Calendar.getInstance();
		sabado.set(2020, Calendar.APRIL, 25);

		// ensinamos o mock a dizer que "hoje" é sabado!
		when(relogio.hoje()).thenReturn(sabado);

		Leilao leilao = new CriadorDeLeilao().para("Playstation").lance(new Usuario("José da Silva"), 2000.0)
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
